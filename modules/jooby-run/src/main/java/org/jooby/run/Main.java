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
package org.jooby.run;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
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
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.LongStream;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.log.ModuleLogger;

public class Main {

  static final String JOOBY_REF = "org.jooby.internal.run__.JoobyRef";

  private static boolean DEBUG;

  private static boolean TRACE;

  static {
    logLevel();
  }

  private AppModuleLoader loader;
  private ExecutorService executor;
  private Watcher scanner;
  private PathMatcher includes;
  private PathMatcher excludes;
  private volatile Object app;
  private AtomicReference<String> hash = new AtomicReference<>("");
  private ModuleIdentifier mId;
  private String mainClass;
  private volatile Module module;
  private List<String> args;
  private AtomicBoolean starting = new AtomicBoolean(false);
  private AtomicInteger counter = new AtomicInteger(0);

  private Path[] watchDirs;

  public Main(final String mId, final String mainClass, final List<File> watchDirs,
      final File... cp) throws Exception {
    this.mainClass = mainClass;
    loader = AppModuleLoader.build(mId, cp);
    this.mId = ModuleIdentifier.create(mId);
    this.watchDirs = toPath(watchDirs);
    this.executor = Executors.newSingleThreadExecutor(task -> new Thread(task, "HotSwap"));
    this.scanner = new Watcher(this::onChange, this.watchDirs);
    includes("**/*.class" + File.pathSeparator + "**/*.conf" + File.pathSeparator
        + "**/*.properties" + File.pathSeparator + "*.js" + File.pathSeparator + "src/*.js");
    excludes("");
  }

  private Path[] toPath(final List<File> watchDir) throws IOException {
    Set<File> files = new LinkedHashSet<>();
    files.add(new File(System.getProperty("user.dir")));
    if (watchDir != null) {
      files.addAll(watchDir);
    }
    List<Path> paths = new ArrayList<>();
    for (File file : files) {
      if (file.exists()) {
        paths.add(file.getCanonicalFile().toPath());
      }
    }
    return paths.toArray(new Path[paths.size()]);
  }

  public static void main(final String[] args) throws Exception {
    List<File> cp = new ArrayList<>();
    List<File> watch = new ArrayList<>();
    String includes = null;
    String excludes = null;

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
          String[] deps = option[1].split(File.pathSeparator);
          for (String dep : deps) {
            cp.add(new File(dep));
          }
          break;
        case "watchdirs":
          String[] dirs = option[1].split(File.pathSeparator);
          for (String dir : dirs) {
            watch.add(new File(dir));
          }
          break;
        default:
          throw new IllegalArgumentException("Unknown option: " + args[i]);
      }
    }
    // set log level, once we call setSystemProps
    logLevel();

    if (cp.isEmpty()) {
      cp.add(new File(System.getProperty("user.dir")));
    }

    Main launcher = new Main(args[0], args[1], watch, cp.toArray(new File[cp.size()]));
    if (includes != null) {
      launcher.includes(includes);
    }
    if (excludes != null) {
      launcher.excludes(excludes);
    }
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

  public void run(final String... args) {
    run(false, args);
  }

  public void run(final boolean block, final String... args) {
    info("Hotswap available on: %s", Arrays.toString(watchDirs));
    info("  includes: %s", includes);
    info("  excludes: %s", excludes);

    this.scanner.start();
    this.args = new ArrayList<>(Arrays.asList(args));
    this.args.add("server.join=false");
    this.args.add("jooby.internal.onStart=" + JOOBY_REF);
    this.startApp(this.args);

    if (block) {
      Object lock = new Object();
      synchronized (lock) {
        // until Ctrl+C
        try {
          lock.wait();
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  @SuppressWarnings("rawtypes")
  private void startApp(final List<String> args) {
    if (starting.get()) {
      return;
    }
    if (app != null) {
      stopApp(app);
    }
    // liveReload hack while restarting
    System.setProperty("joobyRun.counter", String.valueOf(counter.getAndIncrement()));
    starting.set(true);
    debug("scheduling: %s", mainClass);
    executor.submit(() -> {
      String runclass = this.mainClass;
      String alias = runclass;
      ClassLoader ctxLoader = Thread.currentThread().getContextClassLoader();
      try {
        module = loader.loadModule(mId);
        ModuleClassLoader mcloader = module.getClassLoader();

        Thread.currentThread().setContextClassLoader(mcloader);

        debug("starting: %s", runclass);
        Class appref = mcloader.loadClass(JOOBY_REF);
        debug("loaded: %s", appref);
        Class appclass = mcloader.loadClass(runclass);
        debug("loaded: %s", appclass);
        Method main = appclass.getDeclaredMethod("main", String[].class);
        debug("loaded: %s", main);
        Object params = args.toArray(new String[args.size()]);
        debug("calling: %s", main);
        main.invoke(null, params);
        debug("called: %s", main);
        Field reffld = appref.getDeclaredField("ref");
        AtomicReference ref = (AtomicReference) reffld.get(null);
        this.app = ref.get();
        Method started = app.getClass().getMethod("isStarted");
        Boolean success = (Boolean) started.invoke(this.app);
        if (success) {
          debug("started: %s", alias);
        } else {
          debug("not started: %s", alias);
          System.exit(1);
        }
      } catch (Throwable ex) {
        Throwable cause = ex;
        if (ex instanceof InvocationTargetException) {
          cause = ((InvocationTargetException) ex).getTargetException();
        }
        error("%s.start() resulted in error", alias, cause);
      } finally {
        starting.set(false);
        Thread.currentThread().setContextClassLoader(ctxLoader);
      }
    });
  }

  private void stopApp(final Object app) {
    try {
      debug("stopping: %s", mainClass);
      app.getClass().getMethod("stop").invoke(app);
    } catch (Throwable ex) {
      error("%s.stop() resulted in error", mainClass, ex);
    } finally {
      try {
        debug("unloading: %s", mainClass);
        loader.unload(module);
      } catch (Throwable ex) {
        // sshhhh
      }
    }

  }

  public Main includes(final String includes) {
    this.includes = pathMatcher(includes);
    return this;
  }

  public Main excludes(final String excludes) {
    this.excludes = pathMatcher(excludes);
    return this;
  }

  private void onChange(final Kind<?> kind, final Path path) {
    try {
      debug("OnChange: %s(%s)", path, kind);
      Path candidate = relativePath(path);
      if (candidate == null || !includes.matches(candidate) || excludes.matches(candidate)) {
        debug("Ignoring change: %s", path);
        return;
      }
      // weak hash check: avoid change on conf/* that are propagated to target/classs by maven.
      File f = candidate.toFile();
      // len and lastModified reports 0 on external paths, we hack and use now as millis
      long l = LongStream.of(f.length(), f.lastModified(), System.currentTimeMillis())
          .filter(it -> it > 0)
          .findFirst()
          .getAsLong();
      String h = f.getName() + ":" + l;
      debug("hash %s > new hash %s", hash.get(), h);
      if (!hash.getAndSet(h).equals(h)) {
        debug("File change detected: %s", path);
        // reload
        startApp(args);
      } else {
        debug("Ignoring change: %s", path);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private Path relativePath(final Path path) {
    for (Path root : watchDirs) {
      if (path.startsWith(root)) {
        return root.relativize(path);
      }
    }
    return null;
  }

  private static PathMatcher pathMatcher(final String expressions) {
    List<PathMatcher> matchers = new ArrayList<>();
    for (String expression : expressions.split(File.pathSeparator)) {
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

  private static void logLevel() {
    DEBUG = "debug".equalsIgnoreCase(System.getProperty("logLevel", ""));

    TRACE = "trace".equalsIgnoreCase(System.getProperty("logLevel", ""));

    if (TRACE) {
      DEBUG = true;
      Module.setModuleLogger(new ModuleLogger() {

        @Override
        public void trace(final Throwable t, final String format, final Object arg1,
            final Object arg2, final Object arg3) {
          Main.trace(format, arg1, arg2, arg3, t);
        }

        @Override
        public void trace(final Throwable t, final String format, final Object arg1,
            final Object arg2) {
          Main.trace(format, arg1, arg2, t);
        }

        @Override
        public void trace(final String format, final Object arg1, final Object arg2,
            final Object arg3) {
          Main.trace(format, arg1, arg2, arg3);
        }

        @Override
        public void trace(final Throwable t, final String format, final Object... args) {
          Object[] values = new Object[args.length + 1];
          System.arraycopy(args, 0, values, 0, args.length);
          values[values.length - 1] = t;
          Main.trace(format, values);
        }

        @Override
        public void trace(final Throwable t, final String format, final Object arg1) {
          Main.trace(format, arg1, t);
        }

        @Override
        public void trace(final String format, final Object arg1, final Object arg2) {
          Main.trace(format, arg1, arg2);

        }

        @Override
        public void trace(final Throwable t, final String message) {
          Main.trace(message, t);
        }

        @Override
        public void trace(final String format, final Object... args) {
          Main.trace(format, args);
        }

        @Override
        public void trace(final String format, final Object arg1) {
          Main.trace(format, arg1);
        }

        @Override
        public void trace(final String message) {
          Main.trace(message);
        }

        @Override
        public void providerUnloadable(final String name, final ClassLoader loader) {
        }

        @Override
        public void moduleDefined(final ModuleIdentifier identifier,
            final ModuleLoader moduleLoader) {
        }

        @Override
        public void greeting() {
        }

        @Override
        public void classDefined(final String name, final Module module) {
        }

        @Override
        public void classDefineFailed(final Throwable throwable, final String className,
            final Module module) {
        }
      });
    }

    // set logback
    String logback = Optional.ofNullable(System.getProperty("logback.configurationFile"))
        .orElseGet(() -> Arrays
            .asList(Paths.get("conf", "logback-test.xml"), Paths.get("conf", "logback.dev.xml"),
                Paths.get("conf", "logback.xml"))
            .stream()
            .filter(p -> p.toFile().exists())
            .map(Path::toString)
            .findFirst()
            .orElse(Paths.get("conf", "logback.xml").toString()));
    debug("logback: %s", logback);
    System.setProperty("logback.configurationFile", logback);
  }

  public static void info(final String message, final Object... args) {
    System.out.println(format("info", message, args));
  }

  public static void error(final String message, final Object... args) {
    System.err.println(format("error", message, args));
  }

  public static void debug(final String message, final Object... args) {
    if (DEBUG) {
      System.out.println(format("debug", message, args));
    }
  }

  public static void trace(final String message, final Object... args) {
    if (TRACE) {
      System.out.println(format("trace", message, args));
    }
  }

  private static String format(final String level, final String message, final Object... args) {
    Object[] values = args;
    Throwable x = null;
    if (args.length > 0) {
      if (args[args.length - 1] instanceof Throwable) {
        x = (Throwable) args[args.length - 1];
        values = new Object[args.length - 1];
        System.arraycopy(args, 0, values, 0, values.length);
      }
    }
    String msg = String.format(message, values);
    StringBuilder buff = new StringBuilder();
    buff.append(">>> jooby:run[")
        .append(level)
        .append("|")
        .append(Thread.currentThread().getName())
        .append("]: ")
        .append(msg);
    if (x != null) {
      buff.append("\n");
      StringWriter writer = new StringWriter();
      x.printStackTrace(new PrintWriter(writer));
      buff.append(writer);
    }
    return buff.toString();
  }
}
