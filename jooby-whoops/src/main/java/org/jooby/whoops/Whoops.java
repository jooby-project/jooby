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
package org.jooby.whoops;

import static javaslang.API.$;
import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Predicates.instanceOf;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jooby.Env;
import org.jooby.Err;
import org.jooby.Err.Handler;
import org.jooby.Jooby;
import org.jooby.MediaType;
import org.jooby.Route;
import org.jooby.Routes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import com.google.inject.Binder;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.loader.ClasspathLoader;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import com.typesafe.config.Config;

import javaslang.Lazy;
import javaslang.control.Try;

/**
 * <h1>whoops</h1>
 * <p>
 * Pretty error page that helps you debug your web application.
 * </p>
 * <p>
 * <strong>NOTE</strong>: This module is base on <a href="https://github.com/filp/whoops">whoops</a>
 * and uses the same front end resources.
 * </p>
 *
 * <h2>exports</h2>
 * <ul>
 * <li>A {@link Err.Handler pretty error page}</li>
 * </ul>
 *
 * <h2>usage</h2>
 *
 * <pre>{@code
 * {
 *   use(new Whoops());
 *
 *   get("/", req -> {
 *     throw new IllegalStateException("Something broken!");
 *   });
 * }
 * }</pre>
 *
 * <p>
 * The pretty error page handler is available in development mode:
 * <code>application.env = dev</code>
 * </p>
 *
 * <h2>custom err pages</h2>
 * <p>
 * The pretty error page is implemented via {@link Routes#err(Handler)}. You might run into troubles
 * if your application require custom error pages. On those cases you probably won't use this module
 * or if apply one of the following options:
 * </p>
 *
 * <h3>whoops as last err handler</h3>
 * <p>
 * This option is useful if you have custom error pages on some specific exceptions:
 * </p>
 * <pre>{@code
 * {
 *   err(NotFoundException.class, (req, rsp, err) -> {
 *    // custom not found
 *   });
 *
 *   err(AccessDeniedException.class, (req, rsp, err) -> {
 *    // custom access denied
 *   });
 *
 *   // not handled it use whoops
 *   use(new Whoops());
 * }
 * }</pre>
 * <p>
 * Here the custom error page for <code>NotFoundException</code> or
 * <code>AccessDeniedException</code> will be render before the <code>Whoops</code> error handler.
 * </p>
 *
 * <h3>whoops on dev</h3>
 * <p>
 * This options will active <code>Whoops</code> in <code>dev</code> and the custom err pages in
 * <code>prod-like</code> environments:
 * </p>
 *
 * <pre>{@code
 * {
 *   on("dev", () -> {
 *     use(new Whoops());
 *   })
 *   .orElse(() -> {
 *     err((req, rsp, err) -> {
 *       // custom not found
 *     });
 *   });
 * }
 * }</pre>
 *
 * @author edgar
 * @since 1.0.0.CR4
 */
public class Whoops implements Jooby.Module {

  private static final String HANDLER = "org.jooby.internal.HttpHandlerImpl";

  private static int SAMPLE_SIZE = 10;

  private static BiFunction<Path, Integer, String> openWith = (p, l) -> "";

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final int maxFrameSize;

  /**
   * Creates a new {@link Whoops} module.
   *
   * @param maxFrameSize Max number of frame to show in the pretty error page.
   */
  public Whoops(final int maxFrameSize) {
    this.maxFrameSize = maxFrameSize;
  }

  /**
   * Creates a new {@link Whoops} module with max frame size of 8.
   */
  public Whoops() {
    this(8);
  }

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    boolean whoops = conf.hasPath("whoops.enabled")
        ? conf.getBoolean("whoops.enabled")
        : "dev".equals(env.name());
    if (whoops) {
      ClassLoader loader = env.routes().getClass().getClassLoader();
      Handler handler = prettyPage(loader, SourceLocator.local(), maxFrameSize, log);
      env.routes().err(tryPage(handler, log));
    }
  }

  static Handler tryPage(final Handler handler, final Logger log) {
    return (req, rsp, ex) -> Try.run(() -> handler.handle(req, rsp, ex)).onFailure(cause -> {
      log.debug("execution of pretty err page resulted in exception", cause);
    });
  }

  private static Handler prettyPage(final ClassLoader loader, final SourceLocator locator,
      final int maxStackSize, final Logger log) {
    String css = readString(loader, "css/whoops.base.css");
    String clipboard = readString(loader, "js/clipboard.min.js");
    String js = readString(loader, "js/whoops.base.js");
    String zepto = readString(loader, "js/zepto.min.js");

    ClasspathLoader cpathloader = new ClasspathLoader();
    cpathloader.setPrefix("whoops");
    cpathloader.setSuffix(".html");
    PebbleEngine engine = new PebbleEngine.Builder()
        .loader(cpathloader)
        .templateCache(CacheBuilder.newBuilder().maximumSize(10).build())
        .build();

    /** Lazy compile template and keep it */
    Lazy<PebbleTemplate> template = Lazy.of(() -> Try.of(() -> engine.getTemplate("layout")).get());

    return (req, rsp, err) -> {
      // only html, ignore any other request and fallback to default handler
      if (req.accepts(MediaType.html).isPresent()) {
        // is this a real Err? or we just wrap it?
        Throwable cause = Optional.ofNullable(err.getCause()).orElse(err);
        // dump causes as a list
        List<Throwable> causal = Throwables.getCausalChain(cause);

        // get the cause (initial exception)
        Throwable head = causal.get(causal.size() - 1);
        String message = message(head);

        Map<String, Object> envdata = new LinkedHashMap<>();

        envdata.put("response", dump(() -> ImmutableMap.of("status", rsp.status().get())));

        /** route */
        envdata.put("route", dump(() -> {
          Route route = req.route();
          ImmutableMap.Builder<String, Object> map = ImmutableMap.builder();
          return map
              .put("method", route.method())
              .put("path", route.path())
              .put("path vars", route.vars())
              .put("pattern", route.pattern())
              .put("name", route.name())
              .put("attributes", route.attributes()).build();
        }));
        /** request params */
        envdata.put("request params", dump(() -> req.params().toMap()));
        /** request locals */
        envdata.put("request locals", dump(req::attributes));
        /** http headers */
        envdata.put("request headers", dump(req::headers));
        /** session */
        req.ifSession().ifPresent(s -> envdata.put("session", dump(s::attributes)));

        List<Map<String, Object>> frames = causal.stream().filter(it -> it != head)
            .map(it -> frame(loader, locator, it, it.getStackTrace()[0]))
            .collect(Collectors.toList());

        frames.addAll(frames(loader, locator, head));

        // truncate frames
        frames = frames.subList(0, Math.min(maxStackSize, frames.size()));

        Map<String, Object> context = ImmutableMap.<String, Object> builder()
            .put("stylesheet", css)
            .put("zepto", zepto)
            .put("clipboard", clipboard)
            .put("javascript", js)
            .put("chain", causal)
            .put("q", head.getClass().getName() + ": " + message)
            .put("message", message)
            .put("stacktrace", Throwables.getStackTraceAsString(cause))
            .put("frames", frames)
            .put("env", envdata)
            .build();
        Writer writer = new StringWriter();

        template.get().evaluate(writer, context);

        log.error("execution of: {}{} resulted in exception\nRoute:\n{}\n\nStacktrace:",
            req.method(), req.path(), req.route().print(6), err);
        rsp.type(MediaType.html).send(writer.toString());
      }
    };
  }

  private static String message(final Throwable cause) {
    return Match(cause).of(
        Case(instanceOf(Supplier.class), s -> s.get().toString()),
        Case($(), () -> Optional.ofNullable(cause.getMessage()).orElse("")));
  }

  private static <T> Map<String, String> dump(final Supplier<Map<String, T>> hash) {
    return dump(hash, v -> v.toString());
  }

  private static <T> Map<String, String> dump(final Supplier<Map<String, T>> hash,
      final Function<T, String> map) {
    Map<String, String> data = new LinkedHashMap<>();
    hash.get().forEach((n, v) -> data.put(n, map.apply(v)));
    return data;
  }

  static String readString(final ClassLoader loader, final String path) {
    return Try.of(() -> {
      InputStream stream = null;
      try {
        stream = loader.getResourceAsStream("whoops/" + path);
        return new String(ByteStreams.toByteArray(stream), StandardCharsets.UTF_8);
      } finally {
        Closeables.closeQuietly(stream);
      }
    }).get();
  }

  static List<Map<String, Object>> frames(final ClassLoader loader, final SourceLocator locator,
      final Throwable cause) {
    List<StackTraceElement> stacktrace = Arrays.asList(cause.getStackTrace());
    int limit = IntStream.range(0, stacktrace.size())
        .filter(i -> stacktrace.get(i).getClassName().equals(HANDLER)).findFirst()
        .orElse(stacktrace.size());
    return stacktrace.stream()
        // trunk stack at HttpHandlerImpl (skip servers stack)
        .limit(limit)
        .map(e -> frame(loader, locator, cause, e))
        .collect(Collectors.toList());
  }

  @SuppressWarnings("rawtypes")
  static Map<String, Object> frame(final ClassLoader loader, final SourceLocator locator,
      final Throwable cause, final StackTraceElement e) {
    int line = Math.max(e.getLineNumber(), 1);
    String className = e.getClassName();
    SourceLocator.Source source = locator.source(className);
    int[] range = source.range(line, SAMPLE_SIZE);
    int lineStart = range[0];
    int lineNth = line - lineStart;
    Path filePath = source.getPath();
    Optional<Class> clazz = findClass(loader, className);
    String filename = Optional.ofNullable(e.getFileName()).orElse("~unknown");
    return ImmutableMap.<String, Object> builder()
        .put("fileName", new File(filename).getName())
        .put("methodName", Optional.ofNullable(e.getMethodName()).orElse("~unknown"))
        .put("lineNumber", line)
        .put("lineStart", lineStart + 1)
        .put("lineNth", lineNth)
        .put("location", clazz.map(Whoops::locationOf)
            .orElse(new File(filename).getParent()))
        .put("source", source.source(range[0], range[1]))
        .put("open", openWith.apply(filePath, line))
        .put("type", clazz.map(c -> c.getSimpleName()).orElse(new File(filename).getName()))
        .put("comments", Arrays.asList(
            ImmutableMap.of(
                "context", cause.getClass().getName(),
                "text", message(cause))))
        .build();
  }

  @SuppressWarnings("rawtypes")
  static String locationOf(final Class clazz) {
    return Optional.ofNullable(clazz.getResource(clazz.getSimpleName() + ".class"))
        .map(url -> Try.of(() -> {
          String path = url.getPath();
          int i = path.indexOf("!");
          if (i > 0) {
            // jar url
            String jar = path.substring(0, i);
            return jar.substring(Math.max(jar.lastIndexOf('/'), -1) + 1);
          }
          String cfile = clazz.getName().replace(".", "/") + ".class";
          String relativePath = path.replace(cfile, "");
          return new File(System.getProperty("user.dir"))
              .toPath()
              .relativize(Paths.get(relativePath).toFile().getCanonicalFile().toPath())
              .toString();
        }).getOrElse("~unknown"))
        .orElse("~unknown");
  }

  @SuppressWarnings("rawtypes")
  static Optional<Class> findClass(final ClassLoader loader, final String name) {
    return Arrays
        .asList(loader, Thread.currentThread().getContextClassLoader())
        .stream()
        // we don't care about exception
        .map(cl -> Try.<Class> of(() -> cl.loadClass(name)).getOrElse((Class) null))
        .filter(Objects::nonNull)
        .findFirst();
  }

}
