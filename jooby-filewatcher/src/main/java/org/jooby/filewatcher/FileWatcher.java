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
package org.jooby.filewatcher;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jooby.Env;
import org.jooby.Jooby.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import javaslang.CheckedFunction1;
import javaslang.CheckedFunction2;
import javaslang.control.Try.CheckedConsumer;

/**
 * <h1>file watcher</h1>
 * <p>
 * Watches for file system changes or event. It uses a watch service to monitor a directory for
 * changes so that it can update its display of the list of files when files are created or deleted.
 * </p>
 *
 * <h2>usage</h2>
 * <p>
 * Watch <code>mydir</code> and listen for create, modify or delete event on all files under it:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new FileWatcher()
 *     .register(Paths.get("mydir"), (kind, path) -> {
 *       log.info("found {} on {}", kind, path);
 *     });
 *   );
 * }
 * }</pre>
 *
 * <h2>file handler</h2>
 * <p>
 * You can specify a {@link FileEventHandler} instance or class while registering a path:
 * </p>
 *
 * <p>
 * Instance:
 * </p>
 * <pre>{@code
 * {
 *   use(new FileWatcher()
 *     .register(Paths.get("mydir"), (kind, path) -> {
 *       log.info("found {} on {}", kind, path);
 *     });
 *   );
 * }
 * }</pre>
 *
 * <p>
 * Class reference:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new FileWatcher()
 *     .register(Paths.get("mydir"), MyFileEventHandler.class)
 *   );
 * }
 * }</pre>
 *
 * <p>
 * Worth to mention that <code>MyFileEventHandler</code> will be provided by Guice.
 * </p>
 *
 * <h2>options</h2>
 * <p>
 * You can specify a couple of options at registration time:
 * </p>
 * <pre>{@code
 * {
 *   use(new FileWatcher(
 *     .register(Paths.get("mydir"), MyFileEventHandler.class, options -> {
 *       options.kind(StandardWatchEventKinds.ENTRY_MODIFY)
 *              .recursive(false)
 *              .includes("*.java");
 *     });
 *   ));
 * }
 * }</pre>
 *
 * <p>
 * 1. Here we listen for {@link StandardWatchEventKinds#ENTRY_MODIFY} (we don't care about create or
 * delete).
 * </p>
 * <p>
 * 2. We turn off recursive watching, only direct files are detected.
 * </p>
 * <p>
 * 3. We want <code>.java</code> files and we ignore any other files.
 * </p>
 *
 * <h2>configuration file</h2>
 * <p>
 * In addition to do it programmatically, you can do it via configuration properties:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new FileWatcher()
 *     .register("mydirproperty", MyEventHandler.class)
 *   );
 * }
 * }</pre>
 *
 * <p>
 * The <code>mydirproperty</code> property must be present in your <code>.conf</code> file.
 * </p>
 *
 * <p>
 * But of course, you can entirely register paths from <code>.conf</code> file:
 * </p>
 *
 * <pre>{@code
 * filewatcher {
 *   register {
 *     path: "mydir"
 *     handler: "org.example.MyFileEventHandler"
 *     kind: "ENTRY_MODIFY"
 *     includes: "*.java"
 *     recursive: false
 *   }
 * }
 * }</pre>
 *
 * <p>
 * Multiple paths are supported using array notation:
 * </p>
 *
 * <pre>{@code
 * filewatcher {
 *   register: [{
 *     path: "mydir1"
 *     handler: "org.example.MyFileEventHandler"
 *   }, {
 *     path: "mydir2"
 *     handler: "org.example.MyFileEventHandler"
 *   }]
 * }
 * }</pre>
 *
 * <p>
 * Now use the module:
 * </p>
 * <pre>{@code
 * {
 *   use(new FileWatcher());
 * }
 * }</pre>
 *
 * <p>
 * The {@link FileWatcher} module read the <code>filewatcher.register</code> property and setup
 * everything.
 * </p>
 *
 * @author edgar
 */
public class FileWatcher implements Module {
  private static final Consumer<FileEventOptions> EMPTY = it -> {
  };

  /**
   * The logging system.
   */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final List<CheckedFunction2<Config, Binder, FileEventOptions>> bindings = new ArrayList<>();

  private WatchService watcher;

  /**
   * Register the given directory tree and execute the given handler whenever a file/folder has been
   * created, modified or delete.
   *
   * @param path Directory to register.
   * @param handler Handler to execute.
   * @return This module.
   */
  public FileWatcher register(final Path path, final Class<? extends FileEventHandler> handler) {
    return register(path, handler, EMPTY);
  }

  /**
   * Register the given directory tree and execute the given handler whenever a file/folder has been
   * created, modified or delete. The configurer callback allow you to customize default event
   * kinds, filter specific file types and restrict the watch scope lookup to direct files.
   *
   * @param path Directory to register.
   * @param handler Handler to execute.
   * @param configurer Configurer callback.
   * @return This module.
   * @see FileEventOptions
   */
  public FileWatcher register(final Path path, final Class<? extends FileEventHandler> handler,
      final Consumer<FileEventOptions> configurer) {
    return register(c -> path, p -> new FileEventOptions(p, handler), configurer);
  }

  /**
   * Register the given directory tree and execute the given handler whenever a file/folder has been
   * created, modified or delete.
   *
   * @param path Directory to register.
   * @param handler Handler to execute.
   * @return This module.
   */
  public FileWatcher register(final Path path, final FileEventHandler handler) {
    return register(c -> path, p -> new FileEventOptions(p, handler), EMPTY);
  }

  /**
   * Register the given directory tree and execute the given handler whenever a file/folder has been
   * created, modified or delete. The configurer callback allow you to customize default event
   * kinds, filter specific file types and restrict the watch scope lookup to direct files.
   *
   * @param path Directory to register.
   * @param handler Handler to execute.
   * @param configurer Configurer callback.
   * @return This module.
   * @see FileEventOptions
   */
  public FileWatcher register(final Path path, final FileEventHandler handler,
      final Consumer<FileEventOptions> configurer) {
    return register(c -> path, p -> new FileEventOptions(p, handler), configurer);
  }

  /**
   * Register the given directory tree and execute the given handler whenever a file/folder has been
   * created, modified or delete.
   *
   * @param property Directory to register.
   * @param handler Handler to execute.
   * @return This module.
   */
  public FileWatcher register(final String property, final FileEventHandler handler) {
    return register(c -> Paths.get(c.getString(property)), p -> new FileEventOptions(p, handler),
        EMPTY);
  }

  /**
   * Register the given directory tree and execute the given handler whenever a file/folder has been
   * created, modified or delete. The configurer callback allow you to customize default event
   * kinds, filter specific file types and restrict the watch scope lookup to direct files.
   *
   * @param property Directory to register.
   * @param handler Handler to execute.
   * @param configurer Configurer callback.
   * @return This module.
   * @see FileEventOptions
   */
  public FileWatcher register(final String property, final FileEventHandler handler,
      final Consumer<FileEventOptions> configurer) {
    return register(c -> Paths.get(c.getString(property)), p -> new FileEventOptions(p, handler),
        configurer);
  }

  /**
   * Register the given directory tree and execute the given handler whenever a file/folder has been
   * created, modified or delete.
   *
   * @param property Directory to register.
   * @param handler Handler to execute.
   * @return This module.
   */
  public FileWatcher register(final String property,
      final Class<? extends FileEventHandler> handler) {
    return register(property, handler, EMPTY);
  }

  /**
   * Register the given directory tree and execute the given handler whenever a file/folder has been
   * created, modified or delete. The configurer callback allow you to customize default event
   * kinds, filter specific file types and restrict the watch scope lookup to direct files.
   *
   * @param property Directory to register.
   * @param handler Handler to execute.
   * @param configurer Configurer callback.
   * @return This module.
   * @see FileEventOptions
   */
  public FileWatcher register(final String property,
      final Class<? extends FileEventHandler> handler,
      final Consumer<FileEventOptions> configurer) {
    return register(c -> Paths.get(c.getString(property)),
        path -> new FileEventOptions(path, handler), configurer);
  }

  private FileWatcher register(final Function<Config, Path> provider,
      final CheckedFunction1<Path, FileEventOptions> handler,
      final Consumer<FileEventOptions> configurer) {
    bindings.add((conf, binder) -> {
      Path path = provider.apply(conf);
      FileEventOptions options = handler.apply(path);
      configurer.accept(options);
      return register(binder, options);
    });
    return this;
  }

  private FileEventOptions register(final Binder binder, final FileEventOptions options) {
    Multibinder.newSetBinder(binder, FileEventOptions.class)
        .addBinding()
        .toInstance(options);
    return options;
  }

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) throws Throwable {
    this.watcher = FileSystems.getDefault().newWatchService();
    binder.bind(WatchService.class).toInstance(watcher);
    List<FileEventOptions> paths = new ArrayList<>();
    paths(env.getClass().getClassLoader(), conf, "filewatcher.register", options -> {
      paths.add(register(binder, options));
    });
    for (CheckedFunction2<Config, Binder, FileEventOptions> binding : bindings) {
      paths.add(binding.apply(conf, binder));
    }
    binder.bind(FileMonitor.class).asEagerSingleton();
    paths.forEach(it -> log.info("Watching: {}", it));
  }

  protected boolean empty() {
    return bindings.isEmpty();
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  private void paths(final ClassLoader loader, final Config conf, final String name,
      final Consumer<FileEventOptions> callback) throws Throwable {
    list(conf, name, value -> {
      Config coptions = ConfigFactory.parseMap((Map) value);
      Class handler = loader.loadClass(coptions.getString("handler"));
      Path path = Paths.get(coptions.getString("path"));
      FileEventOptions options = new FileEventOptions(path, handler);
      list(coptions, "kind", it -> options.kind(new WatchEventKind(it.toString())));
      list(coptions, "modifier", it -> options.modifier(new WatchEventModifier(it.toString())));
      list(coptions, "includes", it -> options.includes(it.toString()));
      list(coptions, "recursive", it -> options.recursive(Boolean.valueOf(it.toString())));
      callback.accept(options);
    });
  }

  @SuppressWarnings("rawtypes")
  private void list(final Config conf, final String name, final CheckedConsumer<Object> callback)
      throws Throwable {
    if (conf.hasPath(name)) {
      Object value = conf.getAnyRef(name);
      List values = value instanceof List ? (List) value : ImmutableList.of(value);
      for (Object it : values) {
        callback.accept(it);
      }
    }
  }

}
