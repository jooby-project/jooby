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
package org.jooby.livereload;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.jooby.Env;
import org.jooby.Jooby.Module;
import org.jooby.MediaType;
import org.jooby.Route;
import org.jooby.Router;
import org.jooby.WebSocket;
import org.jooby.filewatcher.FileWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Binder;
import com.typesafe.config.Config;

import javaslang.Tuple;
import javaslang.Tuple2;

/**
 * <h1>liveReload</h1>
 * <p>
 * <a href="http://livereload.com">LiveReload</a> monitors changes in the file system. As soon as
 * you save a file, it is preprocessed as
 * needed, and the browser is refreshed.
 * </p>
 * <p>
 * Even cooler, when you change a CSS file or an image, the browser is updated instantly without
 * reloading the page.
 * </p>
 *
 * <h2>usage</h2>
 * <pre>{@code
 * {
 *   use(new Jackson());
 *
 *   use(new LiveReload());
 * }
 * }</pre>
 *
 * <p>
 * This module is available as long you run in development: <code>application.env=dev</code>.
 * </p>
 *
 * <h2>configuration</h2>
 *
 * <h3>browser extension</h3>
 * <p>
 * Install the <a href="http://livereload.com/extensions/">LiveReload browser extension.</a>
 * </p>
 *
 * <h3>livereload.js</h3>
 * <p>
 * Add the <code>livereload.js</code> to your web page.
 * </p>
 *
 * <p>
 * You can do this manually:
 * </p>
 *
 * <pre>{@code
 *  <script src="/livereload.js"></script>
 * }</pre>
 *
 * <p>
 * Or use the <code>liveReload</code> local variable with your preferred template engine. Here is an
 * example using <code>Handlebars</code>:
 * </p>
 *
 * <pre>
 * {{liveReload}}
 * </pre>
 *
 * <h3>json module</h3>
 * <p>
 * The <a href=
 * "http://feedback.livereload.com/knowledgebase/articles/86174-livereload-protocol">LiveReload
 * Protocol</a> run on top of a <code>WebSocket</code> and uses <code>JSON</code> as protocol
 * format. That's is why we also need to install a <code>JSON module</code>, like <a
 * href="http://jooby.org/doc/jackson">Jackson</a>.
 * </p>
 *
 * <h2>watcher</h2>
 * <p>
 * It automatically reload static resources from <code>public</code>, <code>target</code> (Maven
 * projects) or <code>build</code> folders (Gradle projects).
 * </p>
 *
 * <p>
 * Every time a change is detected the websocket send a <code>reload command</code>.
 * </p>
 *
 * <p>
 * That's all folks!!
 * </p>
 *
 * @author edgar
 * @since 1.1.0
 */
public class LiveReload implements Module {

  public static final String V7 = "http://livereload.com/protocols/official-7";

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private static final Set<String> CSS = ImmutableSet.of(".css", ".scss", ".sass", ".less");

  private Predicate<Path> css = path -> CSS.stream()
      .filter(ext -> path.toString().endsWith(ext))
      .findFirst()
      .isPresent();

  private List<Tuple2<Path, List<String>>> paths = new ArrayList<>();

  /**
   * Creates a new {@link LiveReload} module.
   */
  public LiveReload() {
  }

  /**
   * Add the given path to the watcher.
   *
   * @param path Path to watch.
   * @param includes Glob pattern of matching files.
   * @return This module.
   */
  public LiveReload register(final Path path, final String... includes) {
    if (Files.exists(path)) {
      paths.add(Tuple.of(path, Arrays.asList(includes)));
    }
    return this;
  }

  /**
   * Test if a path is a css like file (less, sass, etc.).
   *
   * @param predicate CSS predicate.
   * @return This module.
   */
  public LiveReload liveCss(final Predicate<Path> predicate) {
    this.css = predicate;
    return this;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void configure(final Env env, final Config conf, final Binder binder) throws Throwable {
    boolean enabled = conf.hasPath("livereload.enabled")
        ? conf.getBoolean("livereload.enabled")
        : "dev".equals(env.name());
    if (enabled) {
      Router router = env.router();
      /**
       * Livereload client:
       */
      String livereloadjs = "/" + LiveReload.class.getPackage().getName().replace(".", "/")
          + "/livereload.js";
      router.assets("/livereload.js", livereloadjs);
      /** {{liveReload}} local variable */
      router.use("*", (req, rsp) -> req.set("liveReload",
          "<script src=\"" + req.contextPath() + "/livereload.js\"></script>"))
          .name("livereload");

      String serverName = CaseFormat.LOWER_CAMEL
          .to(CaseFormat.UPPER_CAMEL, conf.getString("application.name"));

      Queue<WebSocket> broadcast = new ConcurrentLinkedQueue<>();
      AtomicBoolean first = new AtomicBoolean(true);
      /**
       * Websocket:
       */
      router.ws("/livereload", ws -> {
        // add to broadcast
        broadcast.add(ws);

        ws.onMessage(msg -> {
          Map<String, Object> cmd = msg.to(Map.class);
          log.debug("command: {}", cmd);

          Map<String, Object> rsp = handshake(cmd, serverName, V7);
          if (rsp != null) {
            log.debug("sending: {}", rsp);
            ws.send(rsp);
          } else {
            log.trace("ignoring command: {}", cmd);
            // resync app state after a jooby:run restart
            if (first.compareAndSet(true, false)) {
              int counter = Integer.parseInt(System.getProperty("joobyRun.counter", "0"));
              if (counter > 0) {
                ws.send(reload("/", true));
              }
            }
          }
        });

        // remove from broadcast
        ws.onClose(reason -> broadcast.remove(ws));
      }).consumes(MediaType.json).produces(MediaType.json);

      if (paths.isEmpty()) {
        register(Paths.get("public"),
            "**/*.css",
            "**/*.scss",
            "**/*.sass",
            "**/*.less",
            "**/*.html",
            "**/*.js",
            "**/*.coffee",
            "**/*.ts");
        register(Paths.get("target"),
            "**/*.class",
            "**/*.conf",
            "**/*.properties");
        register(Paths.get("build"),
            "**/*.class",
            "**/*.conf",
            "**/*.properties");
      }

      FileWatcher watcher = new FileWatcher();
      paths.forEach(it -> watcher.register(it._1, (kind, path) -> {
        Path relative = relative(paths, path.toAbsolutePath());
        log.debug("file changed {}: {}", relative, File.separator);
        Map<String, Object> reload = reload(
            Route.normalize("/" + relative.toString().replace(File.separator, "/")),
            css.test(relative));
        for (WebSocket ws : broadcast) {
          try {
            log.info("sending: {}", reload);
            ws.send(reload);
          } catch (Exception x) {
            log.debug("execution of {} resulted in exception", reload, x);
          }
        }
      }, options -> it._2.forEach(options::includes)));
      watcher.configure(env, conf, binder);
    }
  }

  private boolean isHello(final Map<String, Object> message) {
    return "hello".equals(message.get("command"));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> handshake(final Map<String, Object> client,
      final String serverName, final String version) {
    if (isHello(client)) {
      List<String> protocols = (List<String>) client.get("protocols");
      return protocols.stream()
          .filter(protocol -> version.equalsIgnoreCase(protocol))
          .map(protocol -> {
            Map<String, Object> server = new LinkedHashMap<>();
            server.put("command", "hello");
            server.put("protocols", new String[]{protocol });
            server.put("serverName", serverName);
            return server;
          })
          .findFirst()
          .orElse(null);
    }
    return null;
  }

  private Path relative(final List<Tuple2<Path, List<String>>> paths, final Path path) {
    for (Tuple2<Path, List<String>> meta : paths) {
      Path root = meta._1.toAbsolutePath();
      Path relative = root.relativize(path);
      if (Files.exists(root.resolve(relative))) {
        return relative;
      }
    }
    return path;
  }

  private Map<String, Object> reload(final String path, final boolean liveCss) {
    Map<String, Object> cmd = new LinkedHashMap<>();
    cmd.put("command", "reload");
    cmd.put("path", path);
    cmd.put("liveCSS", liveCss);
    return cmd;
  }

}
