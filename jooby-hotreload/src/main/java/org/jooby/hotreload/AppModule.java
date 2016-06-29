/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.hotreload;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchEvent.Kind;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.log.StreamModuleLogger;

public class AppModule {

  public static boolean DEBUG;

  public static boolean TRACE;

  private AppModuleLoader loader;
  private Path[] paths = {new File(System.getProperty("user.dir")).toPath() };
  private ExecutorService executor;
  private Watcher scanner;
  private PathMatcher includes;
  private PathMatcher excludes;
  private volatile Object app;
  private ModuleIdentifier mId;
  private String mainClass;
  private volatile Module module;

  public AppModule(final String mId, final String mainClass, final File... cp)
      throws Exception {
    this.mainClass = mainClass;
    loader = AppModuleLoader.build(mId, mainClass, cp);
    this.mId = ModuleIdentifier.create(mId);
    this.executor = Executors.newSingleThreadExecutor(task -> new Thread(task, "HotSwap"));
    this.scanner = new Watcher(this::onChange, paths);
  }

  public static void main(final String[] args) throws Exception {
    List<File> cp = new ArrayList<File>();
    String includes = "**/*.class,**/*.conf,**/*.properties,*.js, src/*.js";
    String excludes = "";
    for (int i = 2; i < args.length; i++) {
      String[] option = args[i].split("=");
      if (option.length < 2) {
        throw new IllegalArgumentException("Unknown option: " + args[i]);
      }
      String name = option[0].toLowerCase();
      switch (name) {
        case "includes":
          includes = option[1];
          break;
        case "excludes":
          excludes = option[1];
          break;
        case "props":
          setSystemProperties(new File(option[1]));
          break;
        case "deps":
          String[] deps = option[1].split(":");
          for (String dep : deps) {
            cp.add(new File(dep));
          }
          break;
        default:
          throw new IllegalArgumentException("Unknown option: " + args[i]);
      }
    }
    // set log level, once we call setSystemProps
    DEBUG = Integer.getInteger("logLevel", 2) == 1;
    TRACE = Integer.getInteger("logLevel", 2) == 0;

    if (cp.isEmpty()) {
      cp.add(new File(System.getProperty("user.dir")));
    }

    if (TRACE) {
      Module.setModuleLogger(new StreamModuleLogger(System.out));
    }

    AppModule launcher = new AppModule(args[0], args[1], cp.toArray(new File[cp.size()]))
        .includes(includes)
        .excludes(excludes);
    launcher.run();
  }

  private static void setSystemProperties(final File sysprops) throws IOException {
    try (InputStream in = new FileInputStream(sysprops)) {
      Properties properties = new Properties();
      properties.load(in);
      for (Entry<Object, Object> prop : properties.entrySet()) {
        String name = prop.getKey().toString();
        String value = prop.getValue().toString();
        String existing = System.getProperty(name);
        if (!value.equals(existing)) {
          // set property
          System.setProperty(name, value);
        }
      }
    }
  }

  public void run() {
    System.out.printf("Hotswap available on: %s%n", Arrays.toString(paths));
    System.out.printf("  includes: %s%n", includes);
    System.out.printf("  excludes: %s%n", excludes);

    this.scanner.start();
    this.startApp();
  }

  private void startApp() {
    if (app != null) {
      stopApp(app);
    }
    executor.execute(() -> {
      ClassLoader ctxLoader = Thread.currentThread().getContextClassLoader();
      try {
        module = loader.loadModule(mId);
        ModuleClassLoader mcloader = module.getClassLoader();

        Thread.currentThread().setContextClassLoader(mcloader);

        if (mainClass.equals("org.jooby.Jooby")) {
          // js version
          Object js = mcloader.loadClass("org.jooby.internal.js.JsJooby")
              .newInstance();
          Method runjs = js.getClass().getDeclaredMethod("run", File.class);
          this.app = runjs.invoke(js, new File("app.js"));
        } else {
          this.app = mcloader.loadClass(mainClass)
              .getDeclaredConstructors()[0].newInstance();
        }
        app.getClass().getMethod("start").invoke(app);
      } catch (Throwable ex) {
        System.err.println(mainClass + ".start() resulted in error");
        Throwable cause = ex;
        if (ex instanceof InvocationTargetException) {
          cause = ((InvocationTargetException) ex).getTargetException();
        }
        cause.printStackTrace();
      } finally {
        Thread.currentThread().setContextClassLoader(ctxLoader);
      }
    });
  }

  private void stopApp(final Object app) {
    try {
      app.getClass().getMethod("stop").invoke(app);
    } catch (Throwable ex) {
      System.err.println(app.getClass().getName() + ".stop() resulted in error");
      ex.printStackTrace();
    } finally {
      try {
        loader.unload(module);
      } catch (Throwable ex) {
        // sshhhh
      }
    }

  }

  private AppModule includes(final String includes) {
    this.includes = pathMatcher(includes);
    return this;
  }

  private AppModule excludes(final String excludes) {
    this.excludes = pathMatcher(excludes);
    return this;
  }

  private void onChange(final Kind<?> kind, final Path path) {
    try {
      Path candidate = relativePath(path);
      if (candidate == null || !includes.matches(candidate) || excludes.matches(candidate)) {
        return;
      }
      // reload
      startApp();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private Path relativePath(final Path path) {
    for (Path root : paths) {
      if (path.startsWith(root)) {
        return root.relativize(path);
      }
    }
    return null;
  }

  private static PathMatcher pathMatcher(final String expressions) {
    List<PathMatcher> matchers = new ArrayList<PathMatcher>();
    for (String expression : expressions.split(",")) {
      matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + expression.trim()));
    }
    return new PathMatcher() {

      @Override
      public boolean matches(final Path path) {
        for (PathMatcher matcher : matchers) {
          if (matcher.matches(path)) {
            return true;
          }
        }
        return false;
      }

      @Override
      public String toString() {
        return "[" + expressions + "]";
      }
    };
  }

}
