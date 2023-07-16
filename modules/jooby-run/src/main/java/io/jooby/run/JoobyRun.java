/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.run;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryWatcher;

/**
 * Allow to restart an application on file changes. This lets client to listen for file changes and
 * trigger a restart.
 *
 * <p>This class doesn't compile source code. Instead, let a client (Maven/Gradle) to listen for
 * changes, fire a compilation process and restart the application once compilation finished.
 *
 * @author edgar
 * @since 2.0.0
 */
public class JoobyRun {

  private record Event(long time) {}

  private static class AppModule {
    private final Logger logger;
    private final JoobyModuleLoader loader;
    private final JoobyRunOptions conf;
    private Module module;
    private ClassLoader contextClassLoader;
    private int counter;
    private final AtomicInteger state = new AtomicInteger(CLOSED);
    private static final int CLOSED = 1 << 0;
    private static final int UNLOADING = 1 << 1;
    private static final int UNLOADED = 1 << 2;
    private static final int STARTING = 1 << 3;
    private static final int RESTART = 1 << 4;
    private static final int RUNNING = 1 << 5;

    AppModule(
        Logger logger,
        JoobyModuleLoader loader,
        ClassLoader contextClassLoader,
        JoobyRunOptions conf) {
      this.logger = logger;
      this.loader = loader;
      this.conf = conf;
      this.contextClassLoader = contextClassLoader;
    }

    public Exception start() {
      if (!(state.compareAndSet(CLOSED, STARTING) || state.compareAndSet(UNLOADED, STARTING))) {
        debugState("Jooby already starting.");
        return null;
      }
      try {
        module = loader.loadModule(conf.getProjectName());
        ModuleClassLoader classLoader = module.getClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);

        if (counter == 0) {
          // main class must exists
          module.getClassLoader().loadClass(conf.getMainClass());
        }

        System.setProperty("___jooby_run_hook__", SERVER_REF);
        // Track the number of restarts
        System.setProperty("joobyRun.counter", Integer.toString(counter++));

        Integer port = conf.getPort();
        List<String> args = new ArrayList<>();
        if (port != null) {
          args.add("server.port=" + port);
        }
        module.run(conf.getMainClass(), args.toArray(new String[0]));
      } catch (ClassNotFoundException x) {
        String message = x.getMessage();
        if (message.trim().startsWith(conf.getMainClass())) {
          logger.error(
              "Application class: '{}' not found. Possible solutions:\n"
                  + "  1) Make sure class exists\n"
                  + "  2) Class name is correct (no typo)",
              conf.getMainClass());
          // We must exit the JVM, due it is impossible to guess the main application.
          return new ClassNotFoundException(conf.getMainClass());
        } else {
          printErr(x);
        }
      } catch (Throwable x) {
        printErr(x);
      } finally {
        if (state.compareAndSet(STARTING, RUNNING)) {
          debugState("Jooby is now");
        }
        Thread.currentThread().setContextClassLoader(contextClassLoader);
      }
      // In theory: application started successfully, then something went wrong. Still, users
      // can fix the problem and recompile.
      return null;
    }

    private void printErr(Throwable source) {
      Throwable cause = withoutReflection(source);
      StackTraceElement[] stackTrace = cause.getStackTrace();
      int truncateAt = stackTrace.length;
      for (int i = 0; i < stackTrace.length; i++) {
        StackTraceElement it = stackTrace[i];
        if (it.getClassName().equals("org.jboss.modules.Module")) {
          truncateAt = i;
        }
      }
      if (truncateAt != stackTrace.length) {
        StackTraceElement[] cleanstack = new StackTraceElement[truncateAt];
        System.arraycopy(stackTrace, 0, cleanstack, 0, truncateAt);
        cause.setStackTrace(cleanstack);
      }
      logger.error("execution of {} resulted in exception", conf.getMainClass(), cause);

      // is fatal?
      if (isFatal(source) || isFatal(cause)) {
        sneakyThrow0(source);
      }
    }

    private boolean isFatal(Throwable cause) {
      return cause instanceof InterruptedException
          || cause instanceof LinkageError
          || cause instanceof ThreadDeath
          || cause instanceof VirtualMachineError;
    }

    public boolean isStarting() {
      long s = state.longValue();
      return s > CLOSED && s < RUNNING;
    }

    public void restart() {
      if (state.compareAndSet(RUNNING, RESTART)) {
        closeServer();
        unloadModule();
        start();
      } else {
        debugState("Already restarting.");
      }
    }

    public void close() {
      closeServer();
    }

    private Throwable withoutReflection(Throwable cause) {
      Throwable it = cause;
      Throwable prev = cause;
      while (it instanceof InvocationTargetException) {
        prev = it;
        it = it.getCause();
      }
      return it == null ? prev : it;
    }

    private void unloadModule() {
      if (!state.compareAndSet(CLOSED, UNLOADING)) {
        debugState("Cannot unload as server isn't closed.");
        return;
      }
      try {
        if (module != null) {
          loader.unload(conf.getProjectName(), module);
        }
      } catch (Exception x) {
        logger.debug("unload module resulted in exception", x);
      } finally {
        state.compareAndSet(UNLOADING, UNLOADED);
        module = null;
      }
    }

    private void closeServer() {
      try {
        debugState("Closing server.");
        Class<?> ref = module.getClassLoader().loadClass(SERVER_REF);
        ref.getDeclaredMethod(SERVER_REF_STOP).invoke(null);
      } catch (Exception x) {
        logger.error("Application shutdown resulted in exception", withoutReflection(x));
      } finally {
        state.set(CLOSED);
      }
    }

    private void debugState(String message) {
      if (logger.isDebugEnabled()) {
        String name;
        switch (state.get()) {
          case CLOSED:
            name = "CLOSED";
            break;
          case UNLOADING:
            name = "UNLOADING";
            break;
          case UNLOADED:
            name = "UNLOADED";
            break;
          case STARTING:
            name = "STARTING";
            break;
          case RESTART:
            name = "RESTART";
            break;
          case RUNNING:
            name = "RUNNING";
            break;
          default:
            throw new IllegalStateException("BUG");
        }
        logger.debug(message + " state: {}", name);
      }
    }
  }

  static final String SERVER_REF = "io.jooby.run.ServerRef";

  static final String SERVER_REF_STOP = "stop";

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final JoobyRunOptions options;

  private final Set<Path> resources = new LinkedHashSet<>();

  private final Set<Path> dependencies = new LinkedHashSet<>();

  private DirectoryWatcher watcher;

  private final Map<Path, BiConsumer<String, Path>> watchDirs = new HashMap<>();

  private AppModule module;

  private final Clock clock;

  private final ConcurrentLinkedQueue<Event> queue = new ConcurrentLinkedQueue<>();

  /*
   * How long we wait after the last change before restart
   */
  private final long waitTimeBeforeRestartMillis;

  private final long initialDelayBeforeFirstRestartMillis;

  /**
   * Creates a new instances with the given options.
   *
   * @param options Run options.
   */
  public JoobyRun(JoobyRunOptions options) {
    this.options = options;
    clock = Clock.systemUTC(); // Possibly change for unit test
    waitTimeBeforeRestartMillis = options.getWaitTimeBeforeRestart();
    // this might not need to be configurable
    initialDelayBeforeFirstRestartMillis = JoobyRunOptions.INITIAL_DELAY_BEFORE_FIRST_RESTART;
  }

  /**
   * Add the given path to the project classpath. Path must be a jar or file system directory. File
   * system directory are listen for changes on file changes this method invokes the given callback.
   *
   * @param path Path.
   * @param callback Callback to listen for file changes.
   * @return True if the path was added it to the classpath.
   */
  public boolean addResource(Path path, BiConsumer<String, Path> callback) {
    if (addResource(path)) {
      if (Files.isDirectory(path)) {
        watchDirs.put(path, callback);
      }
      return true;
    }
    return false;
  }

  /**
   * Add the given path to the project classpath. Path must be a jar or file system directory.
   *
   * @param path Path.
   * @return True if the path was added it to the classpath.
   */
  public boolean addResource(Path path) {
    if (Files.exists(path)) {
      if (path.toString().endsWith(".jar")) {
        dependencies.add(path);
      } else {
        resources.add(path);
      }
      return true;
    }
    return false;
  }

  /**
   * Start the application.
   *
   * @throws Throwable If something goes wrong.
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  public void start() throws Throwable {
    this.watcher = newWatcher();
    try {

      logger.debug("project: {}", this);

      /** Allow modules in dev to access classpath while running from maven/gradle: */
      String classPathString =
          Stream.concat(resources.stream(), dependencies.stream())
              .map(Path::toAbsolutePath)
              .map(Path::toString)
              .collect(Collectors.joining(File.pathSeparator));
      System.setProperty("jooby.run.classpath", classPathString);
      var finder = new JoobyModuleFinder(options.getProjectName(), resources, dependencies);

      module =
          new AppModule(
              logger,
              new JoobyModuleLoader(finder),
              Thread.currentThread().getContextClassLoader(),
              options);
      ScheduledExecutorService se;
      Exception error = module.start();
      if (error == null) {
        se = Executors.newScheduledThreadPool(1);
        se.scheduleAtFixedRate(
            this::actualRestart,
            initialDelayBeforeFirstRestartMillis,
            waitTimeBeforeRestartMillis,
            TimeUnit.MILLISECONDS);
        try {
          watcher.watch();
        } finally {
          se.shutdownNow();
        }
      } else {
        // exit
        shutdown();
        throw error;
      }
    } catch (ClosedWatchServiceException expected) {
      logger.trace("Watcher.close resulted in exception", expected);
    }
  }

  /** Restart the application. */
  public void restart() {
    queue.offer(new Event(clock.millis()));
  }

  private synchronized void actualRestart() {
    if (module.isStarting()) {
      return; // We don't empty the queue. This is the case a change was made while starting.
    }
    long t = clock.millis();
    Event e = queue.peek();
    if (e == null) {
      return; // queue was empty
    }
    for (; e != null && (t - e.time) > waitTimeBeforeRestartMillis; e = queue.peek()) {
      queue.poll();
    }
    // e will be null if the queue is empty which means all events were old enough
    if (e == null) {
      module.restart();
    }
  }

  /** Stop and shutdown the application. */
  public void shutdown() {
    if (module != null) {
      module.close();
      module = null;
    }

    if (watcher != null) {
      try {
        watcher.close();
      } catch (Exception x) {
        logger.trace("Watcher.close resulted in exception", x);
      } finally {
        watcher = null;
      }
    }
  }

  private DirectoryWatcher newWatcher() throws IOException {
    List<Path> paths = new ArrayList<>(watchDirs.size());
    paths.addAll(watchDirs.keySet());
    return DirectoryWatcher.builder()
        .paths(paths)
        .listener(event -> onFileChange(event.eventType(), event.path()))
        .build();
  }

  @Override
  public String toString() {
    StringBuilder buff = new StringBuilder();
    buff.append(options.getProjectName()).append("\n");
    buff.append("  watch-dirs: ").append("\n");
    watchDirs.forEach(
        (path, callback) -> buff.append("    ").append(path.toAbsolutePath()).append("\n"));
    buff.append("  build: ").append("\n");
    resources.forEach(it -> buff.append("    ").append(it.toAbsolutePath()).append("\n"));
    buff.append("  dependencies: ").append("\n");
    dependencies.forEach(it -> buff.append("    ").append(it.toAbsolutePath()).append("\n"));
    return buff.toString();
  }

  private void onFileChange(DirectoryChangeEvent.EventType kind, Path path) {
    if (kind == DirectoryChangeEvent.EventType.OVERFLOW) {
      return;
    }
    if (Files.isDirectory(path)) {
      return;
    }
    // Must be a watch directory
    for (Map.Entry<Path, BiConsumer<String, Path>> entry : watchDirs.entrySet()) {
      Path basepath = entry.getKey();
      if (path.startsWith(basepath)) {
        entry.getValue().accept(kind.name(), path);
      }
    }
  }

  @SuppressWarnings("unchecked")
  public static <E extends Throwable> E sneakyThrow0(final Throwable x) throws E {
    throw (E) x;
  }
}
