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
package org.jooby;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchEvent.Kind;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hotswap {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private URLClassLoader loader;

  private volatile Object app;

  private File[] cp;

  private String mainClass;

  private Watcher scanner;

  private ExecutorService executor;

  private Path[] paths;

  private PathMatcher includes;

  private PathMatcher excludes;

  private boolean dcevm;

  public Hotswap(final String mainClass, final File[] cp) throws IOException {
    this.mainClass = mainClass;
    this.cp = cp;
    this.paths = toPath(cp);
    this.executor = Executors.newSingleThreadExecutor();
    this.scanner = new Watcher(this::onChange, paths);
    dcevm = System.getProperty("java.vm.version").toLowerCase().contains("dcevm");
  }

  public static void main(final String[] args) throws Exception {
    List<File> cp = new ArrayList<File>();
    String includes = "**/*.class,**/*.conf,**/*.properties";
    String excludes = "";
    for (int i = 1; i < args.length; i++) {
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
      String[] defcp = {"public", "config", "target/classes" };
      for (String candidate : defcp) {
        File dir = new File(candidate);
        if (dir.exists()) {
          cp.add(dir);
        }
      }
    }
    Hotswap launcher = new Hotswap(args[0], cp.toArray(new File[cp.size()]))
        .includes(includes)
        .excludes(excludes);
    launcher.run();
  }

  private Hotswap includes(final String includes) {
    this.includes = pathMatcher(includes);
    return this;
  }

  private Hotswap excludes(final String excludes) {
    this.excludes = pathMatcher(excludes);
    return this;
  }

  public void run() {
    log.info("Hotswap available on: {}", Arrays.toString(cp));
    log.info("  unlimited runtime class redefinition: {}", dcevm
        ? "yes"
        : "no (see https://github.com/dcevm/dcevm)");
    log.info("  includes: {}", includes);
    log.info("  excludes: {}", excludes);

    this.scanner.start();
    this.startApp();
  }

  private void startApp() {
    if (app != null) {
      stopApp(app);
    }
    executor.execute(() -> {
      ClassLoader old = Thread.currentThread().getContextClassLoader();
      try {
        this.loader = newClassLoader(cp);
        Thread.currentThread().setContextClassLoader(loader);
        this.app = loader.loadClass(mainClass).getDeclaredConstructors()[0].newInstance();
        app.getClass().getMethod("start").invoke(app);
      } catch (InvocationTargetException ex) {
        log.error("Error found while starting: " + mainClass, ex.getCause());
      } catch (Exception ex) {
        log.error("Error found while starting: " + mainClass, ex);
      } finally {
        Thread.currentThread().setContextClassLoader(old);
      }
    });
  }

  private static URLClassLoader newClassLoader(final File[] cp) throws MalformedURLException {
    return new URLClassLoader(toURLs(cp), Hotswap.class.getClassLoader()) {
      @Override
      public String toString() {
        return "AppLoader@" + Arrays.toString(cp);
      }
    };
  }

  private void onChange(final Kind<?> kind, final Path path) {
    try {
      Path candidate = relativePath(path);
      if (candidate == null) {
        log.debug("Can't resolve path: {}... ignoring it", path);
        return;
      }
      if (!includes.matches(path)) {
        log.debug("ignoring file {} -> ~{}", path, includes);
        return;
      }
      if (excludes.matches(path)) {
        log.debug("ignoring file {} -> {}", path, excludes);
        return;
      }
      // reload
      startApp();
    } catch (Exception ex) {
      log.error("Err found while processing: " + path, ex);
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

  private void stopApp(final Object app) {
    try {
      app.getClass().getMethod("stop").invoke(app);
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
        | NoSuchMethodException | SecurityException ex) {
      log.error("couldn't stop app", ex);
    } finally {
      try {
        this.loader.close();
      } catch (IOException ex) {
        log.debug("Can't close classloader", ex);
      }
    }

  }

  private static URL[] toURLs(final File[] cp) throws MalformedURLException {
    URL[] urls = new URL[cp.length];
    for (int i = 0; i < urls.length; i++) {
      urls[i] = cp[i].toURI().toURL();
    }
    return urls;
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
