/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.run;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jooby.internal.run.JoobyModuleFinder;
import io.jooby.internal.run.JoobyModuleLoader;
import io.jooby.internal.run.JoobyMultiModuleFinder;
import io.jooby.internal.run.JoobySingleModuleLoader;
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

  private record Event(Path path, long time, Supplier<Boolean> compileTask) {}

  private static class AppModule {

    private enum State {
      CLOSED,
      UNLOADING,
      UNLOADED,
      STARTING,
      RESTART,
      RUNNING,
      FAILED
    }

    private final Logger logger;
    private final JoobyModuleLoader loader;
    private final JoobyRunOptions conf;
    private Module module;
    private final ClassLoader contextClassLoader;
    private int counter;
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);

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
      if (!(state.compareAndSet(State.CLOSED, State.STARTING)
          || state.compareAndSet(State.UNLOADED, State.STARTING))) {
        debugState("Jooby already starting.");
        return null;
      }

      boolean success = false;

      try {
        module = loader.loadModule(conf.getProjectName());
        ModuleClassLoader classLoader = module.getClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);

        // main class must exists
        var mainClass = module.getClassLoader().loadClass(conf.getMainClass());
        var projectdir = baseDir(conf.getBasedir(), mainClass);
        if (projectdir != null) {
          System.setProperty("jooby.dir", projectdir.toString());
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
        success = true; // Execution reached the end without throwing
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
        if (success) {
          if (state.compareAndSet(State.STARTING, State.RUNNING)) {
            debugState("Jooby is now");
          }
        } else {
          state.set(State.FAILED);
          debugState("Jooby start failed");
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
          || cause instanceof VirtualMachineError;
    }

    public boolean isStarting() {
      State s = state.get();
      return s == State.UNLOADING
          || s == State.UNLOADED
          || s == State.STARTING
          || s == State.RESTART;
    }

    public void restart(boolean unload) {
      // Allow restart if it's currently running OR if it previously failed to start
      if (state.compareAndSet(State.RUNNING, State.RESTART)
          || state.compareAndSet(State.FAILED, State.RESTART)) {
        // Shutdown old state
        closeServer();
        if (unload) {
          unloadModule();
        }

        // Start new state
        start();

        // Run gc asynchronously to clear discarded classloaders without blocking the thread
        CompletableFuture.runAsync(System::gc);
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
      if (!state.compareAndSet(State.CLOSED, State.UNLOADING)) {
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
        state.compareAndSet(State.UNLOADING, State.UNLOADED);
        module = null;
      }
    }

    private void closeServer() {
      try {
        debugState("Closing server.");
        if (module != null) {
          Class<?> ref = module.getClassLoader().loadClass(SERVER_REF);
          ref.getDeclaredMethod(SERVER_REF_STOP).invoke(null);
        }
      } catch (Exception x) {
        logger.error("Application shutdown resulted in exception", withoutReflection(x));
      } finally {
        state.set(State.CLOSED);
      }
    }

    private void debugState(String message) {
      if (logger.isDebugEnabled()) {
        logger.debug("{} state: {}", message, state.get().name());
      }
    }
  }

  public static final String SERVER_REF = "io.jooby.run.ServerRef";

  static final String SERVER_REF_STOP = "stop";

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final JoobyRunOptions options;

  private final Set<Path> classes = new LinkedHashSet<>();
  private final Set<Path> resources = new LinkedHashSet<>();
  private final Set<Path> dependencies = new LinkedHashSet<>();

  private DirectoryWatcher watcher;

  private final Map<Path, BiConsumer<String, Path>> watchDirs = new HashMap<>();

  private AppModule module;

  private final Clock clock;

  private final ConcurrentLinkedQueue<Event> queue = new ConcurrentLinkedQueue<>();

  // Executor and task tracking for the sliding-window debouncer
  private ScheduledExecutorService se;
  private volatile ScheduledFuture<?> scheduledRestart;

  /*
   * How long we wait after the last change before restart
   */
  private final long waitTimeBeforeRestartMillis;

  /**
   * Creates a new instances with the given options.
   *
   * @param options Run options.
   */
  public JoobyRun(JoobyRunOptions options) {
    this.options = options;
    clock = Clock.systemUTC(); // Possibly change for unit test
    waitTimeBeforeRestartMillis = options.getWaitTimeBeforeRestart();
  }

  /**
   * Path must be a jar or file system directory. File system directory are listen for changes on
   * file changes this method invokes the given callback.
   *
   * @param path Path.
   * @param callback Callback to listen for file changes.
   * @return True if the path was added it to the classpath.
   */
  public boolean addWatchDir(Path path, BiConsumer<String, Path> callback) {
    if (Files.exists(path)) {
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
      resources.add(path);
    }
    return false;
  }

  /**
   * Add the given path to the project classpath. Path must be a jar or file system directory.
   *
   * @param path Path.
   * @return True if the path was added it to the classpath.
   */
  public boolean addClasses(Path path) {
    if (Files.exists(path)) {
      classes.add(path);
    }
    return false;
  }

  /**
   * Add the given path to the project classpath. Path must be a jar or file system directory.
   *
   * @param path Path.
   * @return True if the path was added it to the classpath.
   */
  public boolean addJar(Path path) {
    if (Files.exists(path)) {
      dependencies.add(path);
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
          Stream.of(classes, resources, dependencies)
              .flatMap(Set::stream)
              .map(Path::toAbsolutePath)
              .map(Path::toString)
              .collect(Collectors.joining(File.pathSeparator));
      System.setProperty("jooby.run.classpath", classPathString);
      JoobyModuleFinder finder;
      if (options.isUseSingleClassLoader()) {
        finder =
            new JoobySingleModuleLoader(
                options.getProjectName(), classes, resources, dependencies, watchDirs.keySet());
      } else {
        finder =
            new JoobyMultiModuleFinder(
                options.getProjectName(), classes, resources, dependencies, watchDirs.keySet());
      }

      module =
          new AppModule(
              logger,
              new JoobyModuleLoader(finder),
              Thread.currentThread().getContextClassLoader(),
              options);

      Exception error = module.start();
      if (error == null) {
        se = Executors.newScheduledThreadPool(1);
        try {
          watcher.watch();
        } finally {
          if (se != null) {
            se.shutdownNow();
          }
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
  public void restart(Path path) {
    restart(path, null);
  }

  public void restart(Path path, Supplier<Boolean> compileTask) {
    // 1. Queue the event
    queue.offer(new Event(path, clock.millis(), compileTask));

    // 2. Cancel the pending restart if it hasn't executed yet (Sliding Window)
    if (scheduledRestart != null && !scheduledRestart.isDone()) {
      scheduledRestart.cancel(false);
    }

    // 3. Schedule a new restart after the wait period has elapsed since the LAST file change
    if (se != null && !se.isShutdown()) {
      scheduledRestart =
          se.schedule(
              this::processQueueAndRestart, waitTimeBeforeRestartMillis, TimeUnit.MILLISECONDS);
    }
  }

  private void processQueueAndRestart() {
    if (module == null || module.isStarting()) {
      return;
    }

    Event e;
    boolean unload = false;
    Supplier<Boolean> compileTask = null;
    boolean hasEvents = false;

    // Drain the entire queue as a single batch
    while ((e = queue.poll()) != null) {
      hasEvents = true;
      unload = unload || options.isCompileExtension(e.path) || options.isClass(e.path);
      compileTask = Optional.ofNullable(compileTask).orElse(e.compileTask);
    }

    if (!hasEvents) {
      return;
    }

    boolean restart = true;
    if (compileTask != null) {
      restart = compileTask.get();
    }

    if (restart) {
      module.restart(unload);
    }
  }

  /** Stop and shutdown the application. */
  public void shutdown() {
    if (module != null) {
      module.close();
      module = null;
    }

    if (se != null) {
      se.shutdownNow();
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

  static Path baseDir(Path root, Class clazz) {
    try {
      var resource = clazz.getResource(".");
      if (resource != null) {
        if ("file".equals(resource.getProtocol())) {
          var buildFiles = new String[] {"pom.xml", "build.gradle", "build.gradle.kts"};
          var path = new File(resource.toURI()).toPath();
          while (path.startsWith(root)) {

            var buildFile =
                Stream.of(buildFiles)
                    .map(path::resolve)
                    .filter(Files::exists)
                    .findFirst()
                    .orElse(null);
            if (buildFile != null) {
              return buildFile.getParent().toAbsolutePath();
            }
            path = path.getParent();
          }
        }
      }
    } catch (URISyntaxException ignored) {
    }
    return null;
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
    classes.forEach(it -> buff.append("    ").append(it.toAbsolutePath()).append("\n"));
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
