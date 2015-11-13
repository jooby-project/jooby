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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.WatchEvent.Kind;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;

public class AppModule {

  private AppModuleLoader loader;
  private File[] dirs;
  private Path[] paths;
  private ExecutorService executor;
  private Watcher scanner;
  private PathMatcher includes;
  private PathMatcher excludes;
  private volatile Object app;
  private ModuleIdentifier mId;
  private String mainClass;
  private volatile Module module;

  public AppModule(final String mId, final String mainClass, final String repo, final File[] dirs)
      throws IOException {
    this.mainClass = mainClass;
    loader = new AppModuleLoader(new File(repo));
    this.mId = ModuleIdentifier.create(mId);
    this.dirs = dirs;
    this.paths = toPath(dirs);
    this.executor = Executors.newSingleThreadExecutor();
    this.scanner = new Watcher(this::onChange, paths);
  }

  public static void main(final String[] args) throws Exception {
    setPkgs();
    setSystemProperties(args[2]);
    List<File> cp = new ArrayList<File>();
    String includes = "**/*.class,**/*.conf,**/*.properties,*.js, src/*.js";
    String excludes = "";
    for (int i = 3; i < args.length; i++) {
      File dir = new File(args[i]);
      if (dir.exists()) {
        // cp option
        cp.add(dir);
      } else {
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
          default:
            throw new IllegalArgumentException("Unknown option: " + args[i]);
        }
      }
    }
    if (cp.size() == 0) {
      cp.add(new File(System.getProperty("user.dir")));
    }
    AppModule launcher = new AppModule(args[0], args[1], args[2], cp.toArray(new File[cp.size()]))
        .includes(includes)
        .excludes(excludes);
    launcher.run();
  }

  private static void setSystemProperties(final String repo) throws IOException {
    try (InputStream in = new FileInputStream(new File(repo, "sys.properties"))) {
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

  private static void setPkgs() throws IOException {
    Set<String> pkgs = pkgs(new InputStreamReader(AppModule.class.getResourceAsStream("pkgs")));

    /**
     * Hack to let users to configure system packages, javax.transaction cause issues with
     * hibernate.
     */
    pkgs.addAll(pkgs(Paths.get("src", "etc", "jboss-modules", "pkgs.includes").toFile()));

    pkgs.removeAll(pkgs(Paths.get("src", "etc", "jboss-modules", "pkgs.excludes").toFile()));

    StringBuilder sb = new StringBuilder();
    for (String pkg : pkgs) {
      sb.append(pkg).append(',');
    }
    sb.setLength(sb.length() - 1);
    System.setProperty("jboss.modules.system.pkgs", sb.toString());
  }

  private static Set<String> pkgs(final File file) throws IOException {
    if (file.exists()) {
      return pkgs(new FileReader(file));
    }
    return new LinkedHashSet<String>();
  }

  private static Set<String> pkgs(final Reader reader) throws IOException {
    try (BufferedReader in = new BufferedReader(reader)) {
      Set<String> pkgs = new LinkedHashSet<String>();
      String line = in.readLine();
      while (line != null) {
        pkgs.add(line.trim());
        line = in.readLine();
      }
      return pkgs;
    }
  }

  public void run() {
    System.out.printf("Hotswap available on: %s\n", Arrays.toString(dirs));
    System.out.printf("  includes: %s\n", includes);
    System.out.printf("  excludes: %s\n", excludes);

    this.scanner.start();
    this.startApp();
  }

  private void startApp() {
    if (app != null) {
      // System.out.println("stopping ap");
      stopApp(app);
    }
     // System.out.println("scheduling app");
    executor.execute(() -> {
      ClassLoader ctxLoader = Thread.currentThread().getContextClassLoader();
      try {
        // System.out.println("starting app");
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
        // System.out.println("new app " + app);

      } catch (InvocationTargetException ex) {
        System.err.println("Error found while starting: " + mainClass);
        ex.printStackTrace();
      } catch (Exception ex) {
        System.err.println("Error found while starting: " + mainClass);
        ex.printStackTrace();
      } finally {
        Thread.currentThread().setContextClassLoader(ctxLoader);
      }
    });
  }

  private void stopApp(final Object app) {
    try {
      app.getClass().getMethod("stop").invoke(app);
    } catch (Exception ex) {
      System.err.println("couldn't stop app");
      ex.printStackTrace();
    } finally {
      loader.unload(module);
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
      // System.out.println("found: " + path );
      Path candidate = relativePath(path);
      if (candidate == null) {
        // System.("Can't resolve path: {}... ignoring it", path);
        return;
      }
      // System.out.println("resolved: " + path  + " ->" + candidate);
      if (!includes.matches(candidate)) {
        // System.out.printf("ignoring file %s -> ~%s\n", candidate, includes);
        return;
      }
      if (excludes.matches(candidate)) {
        // System.out.printf("ignoring file %s -> %s\n", candidate, excludes);
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

  private static Path[] toPath(final File[] cp) {
    Path[] paths = new Path[cp.length];
    for (int i = 0; i < paths.length; i++) {
      paths[i] = cp[i].toPath();
    }
    return paths;
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
