/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

public class HotSwap {

  private static class Server {
    private Object server;

    public Server(Object server) {
      this.server = server;
    }

    public void stop()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
      Method stop = server.getClass().getDeclaredMethod("stop");
      stop.invoke(server);
    }
  }

  private static class App {
    private Object app;

    public App(Object app) {
      this.app = app;
    }

    private Server start()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
      Method start = app.getClass().getMethod("start");
      return new Server(start.invoke(app));
    }
  }

  private static class ExtModuleLoader extends ModuleLoader {

    public ExtModuleLoader(ModuleFinder... finders) {
      super(finders);
    }

    public void unload(String name, final Module module) {
      super.unloadModuleLocal(name, module);
    }
  }

  private static class AppModule {
    private final Logger logger;
    private final ExtModuleLoader loader;
    private final String projectName;
    private String mainClass;
    private final String executionMode;
    private Module module;
    private Server server;
    private ClassLoader contextClassLoader;
    private int port;

    public AppModule(Logger logger, ExtModuleLoader loader, String projectName, String mainClass,
        int port, String executionMode, ClassLoader contextClassLoader) {
      this.logger = logger;
      this.loader = loader;
      this.projectName = projectName;
      this.mainClass = mainClass;
      this.port = port;
      this.executionMode = executionMode;
      this.contextClassLoader = contextClassLoader;
    }

    public void start() {
      try {
        module = loader.loadModule(projectName);

        ModuleClassLoader classLoader = module.getClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);

        Class joobyClass = classLoader.loadClass("io.jooby.Jooby");
        Class executionModeClass = classLoader.loadClass("io.jooby.ExecutionMode");
        Method createApp = joobyClass
            .getDeclaredMethod("createApp", String[].class, executionModeClass, Class.class);

        Class appClass = classLoader.loadClass(mainClass);
        Enum executionModeValue = Enum.valueOf(executionModeClass, executionMode.toUpperCase());
        App app = new App(createApp
            .invoke(null, new String[]{"server.port=" + port}, executionModeValue, appClass));
        server = app.start();
      } catch (Exception x) {
        logger.error("execution of {} resulted in exception", mainClass, withoutReflection(x));
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
          loader.unload(projectName, module);
        }
      } catch (Exception x) {
        logger.debug("unload module resulted in exception", x);
      } finally {
        module = null;
      }
    }

    private void closeServer() {
      try {
        if (server != null) {
          server.stop();
        }
      } catch (Exception x) {
        logger.error("Application shutdown resulted in exception", x);
      } finally {
        server = null;
      }
    }
  }

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final String projectName;

  private final String mainClass;

  private final String executionMode;

  private final Set<Path> resources = new LinkedHashSet<>();

  private final Set<Path> dependencies = new LinkedHashSet<>();

  private DirectoryWatcher watcher;

  private final Map<Path, BiConsumer<String, Path>> watchDirs = new HashMap<>();

  private AppModule module;

  private int port = 8080;

  public HotSwap(String projectName, String mainClass, String executionMode) {
    this.projectName = projectName;
    if (mainClass.endsWith("Kt")) {
      // Assume it is a kotlin class.
      this.mainClass = mainClass.substring(0, mainClass.length() - 2);
    } else {
      this.mainClass = mainClass;
    }
    this.executionMode = executionMode;
  }

  public boolean addResource(Path path, BiConsumer<String, Path> callback) {
    if (addResource(path)) {
      if (Files.isDirectory(path)) {
        watchDirs.put(path, callback);
      }
      return true;
    }
    return false;
  }

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

  public void start() throws Exception {
    this.watcher = newWatcher();
    try {
      logger.debug("project: {}", toString());

      ModuleFinder[] finders = {new FlattenClasspath(projectName, resources, dependencies)};

      ExtModuleLoader loader = new ExtModuleLoader(finders);
      module = new AppModule(logger, loader, projectName, mainClass, port, executionMode,
          Thread.currentThread().getContextClassLoader());
      module.start();
      watcher.watch();
    } catch (ClosedWatchServiceException expected) {
      logger.trace("Watcher.close resulted in exception", expected);
    }
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void restart() {
    module.restart();
  }

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
    buff.append(projectName).append("\n");
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
