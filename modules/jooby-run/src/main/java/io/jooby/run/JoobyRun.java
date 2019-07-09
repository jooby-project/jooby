/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.run;

import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryWatcher;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleFinder;
import org.jboss.modules.ModuleLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Allow to restart an application on file changes. This lets client to listen for file changes
 * and trigger a restart.
 *
 * This class doesn't compile source code. Instead, let a client (Maven/Gradle) to listen for
 * changes, fire a compilation process and restart the application once compilation finished.
 *
 * @author edgar
 * @since 2.0.0
 */
public class JoobyRun {

  private static class ExtModuleLoader extends ModuleLoader {

    ExtModuleLoader(ModuleFinder... finders) {
      super(finders);
    }

    public void unload(String name, final Module module) {
      super.unloadModuleLocal(name, module);
    }
  }

  private static class AppModule {
    private final Logger logger;
    private final ExtModuleLoader loader;
    private final JoobyRunOptions conf;
    private Module module;
    private ClassLoader contextClassLoader;
    private int counter;

    AppModule(Logger logger, ExtModuleLoader loader, ClassLoader contextClassLoader,
        JoobyRunOptions conf) {
      this.logger = logger;
      this.loader = loader;
      this.conf = conf;
      this.contextClassLoader = contextClassLoader;
    }

    public void start() {
      try {
        System.setProperty("___jooby_run_hook__", ServerRef.class.getName());
        // Track the number of restarts
        System.setProperty("joobyRun.counter", Integer.toString(counter++));

        module = loader.loadModule(conf.getProjectName());
        ModuleClassLoader classLoader = module.getClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);

        module.run(conf.getMainClass(),
            new String[]{"server.port=" + conf.getPort(), "server.join=false"});

      } catch (Exception x) {
        logger.error("execution of {} resulted in exception", conf.getMainClass(),
            withoutReflection(x));
      } finally {
        Thread.currentThread().setContextClassLoader(contextClassLoader);
      }
    }

    public void restart() {
      closeServer();
      unloadModule();
      start();
    }

    public void close() {
      closeServer();
    }

    private Throwable withoutReflection(Throwable cause) {
      while (cause instanceof ReflectiveOperationException) {
        cause = cause.getCause();
      }
      return cause;
    }

    private void unloadModule() {
      try {
        if (module != null) {
          loader.unload(conf.getProjectName(), module);
        }
      } catch (Exception x) {
        logger.debug("unload module resulted in exception", x);
      } finally {
        module = null;
      }
    }

    private void closeServer() {
      try {
        ServerRef.stopServer();
      } catch (Exception x) {
        logger.error("Application shutdown resulted in exception", withoutReflection(x));
      }
    }
  }

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final JoobyRunOptions options;

  private final Set<Path> resources = new LinkedHashSet<>();

  private final Set<Path> dependencies = new LinkedHashSet<>();

  private DirectoryWatcher watcher;

  private final Map<Path, BiConsumer<String, Path>> watchDirs = new HashMap<>();

  private AppModule module;

  /**
   * Creates a new instances with the given options.
   *
   * @param options Run options.
   */
  public JoobyRun(JoobyRunOptions options) {
    this.options = options;
  }

  /**
   * Add the given path to the project classpath. Path must be a jar or file system directory.
   * File system directory are listen for changes on file changes this method invokes the given
   * callback.
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
   * @throws Exception If something goes wrong.
   */
  public void start() throws Exception {
    this.watcher = newWatcher();
    try {
      logger.debug("project: {}", toString());

      ModuleFinder[] finders = {
          new FlattenClasspath(options.getProjectName(), resources, dependencies)};

      ExtModuleLoader loader = new ExtModuleLoader(finders);
      module = new AppModule(logger, loader, Thread.currentThread().getContextClassLoader(),
          options);
      module.start();
      watcher.watch();
    } catch (ClosedWatchServiceException expected) {
      logger.trace("Watcher.close resulted in exception", expected);
    }
  }

  /**
   * Restart the application.
   */
  public void restart() {
    module.restart();
  }

  /**
   * Stop and shutdown the application.
   */
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

  @Override public String toString() {
    StringBuilder buff = new StringBuilder();
    buff.append(options.getProjectName()).append("\n");
    buff.append("  watch-dirs: ").append("\n");
    watchDirs.forEach(
        (path, callback) -> buff.append("    ").append(path.toAbsolutePath()).append("\n"));
    buff.append("  build: ").append("\n");
    resources.forEach(it -> buff.append("    ").append(it.toAbsolutePath()).append("\n"));
    buff.append("  dependencies: ").append("\n");
    dependencies
        .forEach(it -> buff.append("    ").append(it.toAbsolutePath()).append("\n"));
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
}
