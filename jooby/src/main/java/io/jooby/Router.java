/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.typesafe.config.Config;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.buffer.BufferedOutputFactory;
import io.jooby.exception.MissingValueException;
import io.jooby.handler.AssetHandler;
import io.jooby.handler.AssetSource;
import io.jooby.value.ValueFactory;

/**
 * Routing DSL functions.
 *
 * @since 2.0.0
 * @author edgar
 */
public interface Router extends Registry {

  /** Find route result. */
  interface Match {
    /**
     * True for matching route.
     *
     * @return True for matching route.
     */
    boolean matches();

    /**
     * Matched route.
     *
     * @return Matched route.
     */
    @NonNull Route route();

    /**
     * Executes matched route.
     *
     * @param context not null.
     * @param pipeline Handler.
     */
    Object execute(@NonNull Context context, @NonNull Route.Handler pipeline);

    default Object execute(@NonNull Context context) {
      return execute(context, route().getPipeline());
    }

    /**
     * Path pattern variables.
     *
     * @return Path pattern variables.
     */
    @NonNull Map<String, String> pathMap();
  }

  /** HTTP GET. */
  String GET = "GET";

  /** HTTP POST. */
  String POST = "POST";

  /** HTTP PUT. */
  String PUT = "PUT";

  /** HTTP DELETE. */
  String DELETE = "DELETE";

  /** HTTP PATCH. */
  String PATCH = "PATCH";

  /** HTTP HEAD. */
  String HEAD = "HEAD";

  /** HTTP OPTIONS. */
  String OPTIONS = "OPTIONS";

  /** HTTP TRACE. */
  String TRACE = "TRACE";

  /** HTTP Methods. */
  List<String> METHODS = List.of(GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS, TRACE);

  /** Web socket. */
  String WS = "WS";

  /** Sever-Sent events. */
  String SSE = "SSE";

  /**
   * Application configuration.
   *
   * @return Application configuration.
   */
  @NonNull Config getConfig();

  /**
   * Application environment.
   *
   * @return Application environment.
   */
  @NonNull Environment getEnvironment();

  /**
   * Returns the supported locales.
   *
   * @return The supported locales.
   */
  @NonNull List<Locale> getLocales();

  /**
   * Mutable map of application attributes.
   *
   * @return Mutable map of application attributes.
   */
  @NonNull Map<String, Object> getAttributes();

  /**
   * Get an attribute by his key. This is just a utility method around {@link #getAttributes()}.
   *
   * @param key Attribute key.
   * @param <T> Attribute type.
   * @return Attribute value.
   */
  @NonNull default <T> T attribute(@NonNull String key) {
    T attribute = (T) getAttributes().get(key);
    if (attribute == null) {
      throw new MissingValueException(key);
    }
    return attribute;
  }

  /**
   * Set an application attribute.
   *
   * @param key Attribute key.
   * @param value Attribute value.
   * @return This router.
   */
  @NonNull default Router attribute(@NonNull String key, Object value) {
    getAttributes().put(key, value);
    return this;
  }

  /**
   * Application service registry. Services are accessible via this registry or {@link
   * Jooby#require(Class)} calls.
   *
   * <p>This method returns a mutable registry. You are free to modify/alter the registry.
   *
   * @return Service registry.
   */
  @NonNull ServiceRegistry getServices();

  /**
   * Server options. They might be null during application initialization. Once deployed, they are
   * never null (at runtime).
   *
   * @return Server options or <code>null</code>.
   */
  @Nullable ServerOptions getServerOptions();

  /**
   * Set application context path. Context path is the base path for all routes. Default is: <code>/
   * </code>.
   *
   * @param contextPath Context path.
   * @return This router.
   */
  @NonNull Router setContextPath(@NonNull String contextPath);

  /**
   * Get application context path (a.k.a as base path).
   *
   * @return Application context path (a.k.a as base path).
   */
  @NonNull String getContextPath();

  /**
   * When true handles X-Forwarded-* headers by updating the values on the current context to match
   * what was sent in the header(s).
   *
   * <p>This should only be installed behind a reverse proxy that has been configured to send the
   * <code>X-Forwarded-*</code> header, otherwise a remote user can spoof their address by sending a
   * header with bogus values.
   *
   * <p>The headers that are read/set are:
   *
   * <ul>
   *   <li>X-Forwarded-For: Set/update the remote address {@link Context#setRemoteAddress(String)}.
   *   <li>X-Forwarded-Proto: Set/update request scheme {@link Context#setScheme(String)}.
   *   <li>X-Forwarded-Host: Set/update the request host {@link Context#setHost(String)}.
   *   <li>X-Forwarded-Port: Set/update the request port {@link Context#setPort(int)}.
   * </ul>
   *
   * @return True when enabled. Default is false.
   */
  boolean isTrustProxy();

  boolean isStarted();

  boolean isStopped();

  /**
   * When true handles X-Forwarded-* headers by updating the values on the current context to match
   * what was sent in the header(s).
   *
   * <p>This should only be installed behind a reverse proxy that has been configured to send the
   * <code>X-Forwarded-*</code> header, otherwise a remote user can spoof their address by sending a
   * header with bogus values.
   *
   * <p>The headers that are read/set are:
   *
   * <ul>
   *   <li>X-Forwarded-For: Set/update the remote address {@link Context#setRemoteAddress(String)}.
   *   <li>X-Forwarded-Proto: Set/update request scheme {@link Context#setScheme(String)}.
   *   <li>X-Forwarded-Host: Set/update the request host {@link Context#setHost(String)}.
   *   <li>X-Forwarded-Port: Set/update the request port {@link Context#setPort(int)}.
   * </ul>
   *
   * @param trustProxy True to enable.
   * @return This router.
   */
  @NonNull Router setTrustProxy(boolean trustProxy);

  /**
   * Provides a way to override the current HTTP method. Request must be:
   *
   * <p>- POST Form/multipart request
   *
   * <p>For alternative strategy use the {@link #setHiddenMethod(Function)} method.
   *
   * @param parameterName Form field name.
   * @return This router.
   */
  @NonNull Router setHiddenMethod(@NonNull String parameterName);

  /**
   * Provides a way to override the current HTTP method using lookup strategy.
   *
   * @param provider Lookup strategy.
   * @return This router.
   */
  @NonNull Router setHiddenMethod(@NonNull Function<Context, Optional<String>> provider);

  /**
   * Provides a way to set the current user from a {@link Context}. Current user can be retrieve it
   * using {@link Context#getUser()}.
   *
   * @param provider User provider/factory.
   * @return This router.
   */
  @NonNull Router setCurrentUser(@NonNull Function<Context, Object> provider);

  /**
   * If enabled, allows to retrieve the {@link Context} object associated with the current request
   * via the service registry while the request is being processed.
   *
   * @param contextAsService whether to enable or disable this feature
   * @return This router.
   */
  @NonNull Router setContextAsService(boolean contextAsService);

  /* ***********************************************************************************************
   * use(Router)
   * ***********************************************************************************************
   */

  /**
   * Enabled routes for specific domain. Domain matching is done using the <code>host</code> header.
   *
   * <pre>{@code
   * {
   *   domain("foo.com", new FooApp());
   *   domain("bar.com", new BarApp());
   * }
   * }</pre>
   *
   * NOTE: if you run behind a reverse proxy you might to enabled {@link #setTrustProxy(boolean)}.
   *
   * <p>NOTE: ONLY routes are imported. Services, callback, etc.. are ignored.
   *
   * @param domain Predicate
   * @param subrouter Subrouter.
   * @return This router.
   */
  @NonNull Router domain(@NonNull String domain, @NonNull Router subrouter);

  /**
   * Enabled routes for specific domain. Domain matching is done using the <code>host</code> header.
   *
   * <pre>{@code
   * {
   *   domain("foo.com", () -> {
   *     get("/", ctx -> "foo");
   *   });
   *   domain("bar.com", () -> {
   *     get("/", ctx -> "bar");
   *   });
   * }
   * }</pre>
   *
   * NOTE: if you run behind a reverse proxy you might to enabled {@link #setTrustProxy(boolean)}.
   *
   * @param domain Predicate
   * @param body Route action.
   * @return This router.
   */
  @NonNull RouteSet domain(@NonNull String domain, @NonNull Runnable body);

  /**
   * Import routes from given router. Predicate works like a filter and only when predicate pass the
   * routes match against the current request.
   *
   * <p>Example of domain predicate filter:
   *
   * <pre>{@code
   * {
   *   use(ctx -> ctx.getHost().equals("foo.com"), new FooApp());
   *   use(ctx -> ctx.getHost().equals("bar.com"), new BarApp());
   * }
   * }</pre>
   *
   * Imported routes are matched only when predicate pass.
   *
   * <p>NOTE: if you run behind a reverse proxy you might to enabled {@link
   * #setTrustProxy(boolean)}.
   *
   * <p>NOTE: ONLY routes are imported. Services, callback, etc.. are ignored.
   *
   * @param predicate Context predicate.
   * @param router Router to import.
   * @return This router.
   */
  @NonNull Router mount(@NonNull Predicate<Context> predicate, @NonNull Router router);

  /**
   * Import routes from given action. Predicate works like a filter and only when predicate pass the
   * routes match against the current request.
   *
   * <p>Example of domain predicate filter:
   *
   * <pre>{@code
   * {
   *   mount(ctx -> ctx.getHost().equals("foo.com"), () -> {
   *     get("/", ctx -> "foo");
   *   });
   *   mount(ctx -> ctx.getHost().equals("bar.com"), () -> {
   *     get("/", ctx -> "bar");
   *   });
   * }
   * }</pre>
   *
   * NOTE: if you run behind a reverse proxy you might to enabled {@link #setTrustProxy(boolean)}.
   *
   * <p>NOTE: ONLY routes are imported. Services, callback, etc.. are ignored.
   *
   * @param predicate Context predicate.
   * @param body Route action.
   * @return This router.
   */
  @NonNull RouteSet mount(@NonNull Predicate<Context> predicate, @NonNull Runnable body);

  /**
   * Import all routes from the given router and prefix them with the given path.
   *
   * <p>NOTE: ONLY routes are imported. Services, callback, etc.. are ignored.
   *
   * @param path Prefix path.
   * @param router Router to import.
   * @return This router.
   */
  @NonNull Router mount(@NonNull String path, @NonNull Router router);

  /**
   * Import all routes from the given router.
   *
   * <p>NOTE: ONLY routes are imported. Services, callback, etc.. are ignored.
   *
   * @param router Router to import.
   * @return This router.
   */
  @NonNull Router mount(@NonNull Router router);

  /* ***********************************************************************************************
   * Mvc
   * ***********************************************************************************************
   */

  /**
   * Import all routes from the given controller class.
   *
   * @param router Router extension.
   * @return This router.
   */
  @NonNull Router mvc(@NonNull Extension router);

  /**
   * Add a websocket handler.
   *
   * @param pattern WebSocket path pattern.
   * @param handler WebSocket handler.
   * @return A new route.
   */
  @NonNull Route ws(@NonNull String pattern, @NonNull WebSocket.Initializer handler);

  /**
   * Add a server-sent event handler.
   *
   * @param pattern Path pattern.
   * @param handler Handler.
   * @return A new route.
   */
  @NonNull Route sse(@NonNull String pattern, @NonNull ServerSentEmitter.Handler handler);

  /**
   * Returns all routes.
   *
   * @return All routes.
   */
  @NonNull List<Route> getRoutes();

  /**
   * Register a route response encoder.
   *
   * @param encoder MessageEncoder instance.
   * @return This router.
   */
  @NonNull Router encoder(@NonNull MessageEncoder encoder);

  /**
   * Register a route response encoder.
   *
   * @param contentType Accept header should match the content-type.
   * @param encoder MessageEncoder instance.
   * @return This router.
   */
  @NonNull Router encoder(@NonNull MediaType contentType, @NonNull MessageEncoder encoder);

  /**
   * Application temporary directory. This method initialize the {@link Environment} when isn't set
   * manually.
   *
   * @return Application temporary directory.
   */
  @NonNull Path getTmpdir();

  /**
   * Register a decoder for the given content type.
   *
   * @param contentType Content type to match.
   * @param decoder MessageDecoder.
   * @return This router.
   */
  @NonNull Router decoder(@NonNull MediaType contentType, @NonNull MessageDecoder decoder);

  /**
   * Returns the worker thread pool. This thread pool is used to run application blocking code.
   *
   * @return Worker thread pool.
   */
  @NonNull Executor getWorker();

  /**
   * Set a worker thread pool. This thread pool is used to run application blocking code.
   *
   * @param worker Worker thread pool.
   * @return This router.
   */
  @NonNull Router setWorker(@NonNull Executor worker);

  /**
   * Set the default worker thread pool. Via this method the underlying web server set/suggests the
   * worker thread pool that should be used it.
   *
   * <p>A call to {@link #getWorker()} returns the default thread pool, unless you explicitly set
   * one.
   *
   * @param worker Default worker thread pool.
   * @return This router.
   */
  @NonNull Router setDefaultWorker(@NonNull Executor worker);

  @NonNull BufferedOutputFactory getOutputFactory();

  @NonNull Router setOutputFactory(@NonNull BufferedOutputFactory outputFactory);

  /**
   * Attach a filter to the route pipeline.
   *
   * @param filter Filter.
   * @return This router.
   */
  @NonNull Router use(@NonNull Route.Filter filter);

  /**
   * Add a before route decorator to the route pipeline.
   *
   * @param before Before decorator.
   * @return This router.
   */
  @NonNull Router before(@NonNull Route.Before before);

  /**
   * Add an after route decorator to the route pipeline.
   *
   * @param after After decorator.
   * @return This router.
   */
  @NonNull Router after(@NonNull Route.After after);

  /**
   * Dispatch route pipeline to the {@link #getWorker()} worker thread pool. After dispatch
   * application code is allowed to do blocking calls.
   *
   * @param body Dispatch body.
   * @return This router.
   */
  @NonNull Router dispatch(@NonNull Runnable body);

  /**
   * Dispatch route pipeline to the given executor. After dispatch application code is allowed to do
   * blocking calls.
   *
   * @param executor Executor. {@link java.util.concurrent.ExecutorService} instances automatically
   *     shutdown at application exit.
   * @param body Dispatch body.
   * @return This router.
   */
  @NonNull Router dispatch(@NonNull Executor executor, @NonNull Runnable body);

  /**
   * Group one or more routes. Useful for applying cross cutting concerns to the enclosed routes.
   *
   * @param body Route body.
   * @return All routes created.
   */
  @NonNull RouteSet routes(@NonNull Runnable body);

  /**
   * Group one or more routes under a common path prefix. Useful for applying cross-cutting concerns
   * to the enclosed routes.
   *
   * @param pattern Path pattern.
   * @param body Route body.
   * @return All routes created.
   */
  @NonNull RouteSet path(@NonNull String pattern, @NonNull Runnable body);

  /**
   * Add a HTTP GET handler.
   *
   * @param pattern Path pattern.
   * @param handler Application code.
   * @return A route.
   */
  @NonNull default Route get(@NonNull String pattern, @NonNull Route.Handler handler) {
    return route(GET, pattern, handler);
  }

  /**
   * Add a HTTP POST handler.
   *
   * @param pattern Path pattern.
   * @param handler Application code.
   * @return A route.
   */
  @NonNull default Route post(@NonNull String pattern, @NonNull Route.Handler handler) {
    return route(POST, pattern, handler);
  }

  /**
   * Add a HTTP PUT handler.
   *
   * @param pattern Path pattern.
   * @param handler Application code.
   * @return A route.
   */
  @NonNull default Route put(@NonNull String pattern, @NonNull Route.Handler handler) {
    return route(PUT, pattern, handler);
  }

  /**
   * Add a HTTP DELETE handler.
   *
   * @param pattern Path pattern.
   * @param handler Application code.
   * @return A route.
   */
  @NonNull default Route delete(@NonNull String pattern, @NonNull Route.Handler handler) {
    return route(DELETE, pattern, handler);
  }

  /**
   * Add a HTTP PATCH handler.
   *
   * @param pattern Path pattern.
   * @param handler Application code.
   * @return A route.
   */
  @NonNull default Route patch(@NonNull String pattern, @NonNull Route.Handler handler) {
    return route(PATCH, pattern, handler);
  }

  /**
   * Add a HTTP HEAD handler.
   *
   * @param pattern Path pattern.
   * @param handler Application code.
   * @return A route.
   */
  @NonNull default Route head(@NonNull String pattern, @NonNull Route.Handler handler) {
    return route(HEAD, pattern, handler);
  }

  /**
   * Add a HTTP OPTIONS handler.
   *
   * @param pattern Path pattern.
   * @param handler Application code.
   * @return A route.
   */
  @NonNull default Route options(@NonNull String pattern, @NonNull Route.Handler handler) {
    return route(OPTIONS, pattern, handler);
  }

  /**
   * Add a HTTP TRACE handler.
   *
   * @param pattern Path pattern.
   * @param handler Application code.
   * @return A route.
   */
  @NonNull default Route trace(@NonNull String pattern, @NonNull Route.Handler handler) {
    return route(TRACE, pattern, handler);
  }

  /**
   * Add a static resource handler. Static resources are resolved from file system.
   *
   * @param pattern Path pattern.
   * @param source File system directory.
   * @return A route.
   */
  default @NonNull AssetHandler assets(@NonNull String pattern, @NonNull Path source) {
    return assets(pattern, AssetSource.create(source));
  }

  /**
   * Add a static resource handler. Static resources are resolved from:
   *
   * <p>- file-system if the source folder exists in the current user directory - or fallback to
   * classpath when file-system folder doesn't exist.
   *
   * <p>NOTE: This method choose file-system or classpath, it doesn't merge them.
   *
   * @param pattern Path pattern.
   * @param source File-System folder when exists, or fallback to a classpath folder.
   * @return AssetHandler.
   */
  default @NonNull AssetHandler assets(@NonNull String pattern, @NonNull String source) {
    Path path =
        Stream.of(source.split("/"))
            .reduce(Paths.get(System.getProperty("user.dir")), Path::resolve, Path::resolve);
    if (Files.exists(path)) {
      return assets(pattern, path);
    }
    return assets(pattern, AssetSource.create(getClass().getClassLoader(), source));
  }

  /**
   * Add a static resource handler.
   *
   * @param pattern Path pattern.
   * @param source Asset source.
   * @param sources additional Asset sources.
   * @return A route.
   */
  default @NonNull AssetHandler assets(
      @NonNull String pattern, @NonNull AssetSource source, @NonNull AssetSource... sources) {
    AssetSource[] allSources;
    if (sources.length == 0) {
      allSources = new AssetSource[] {source};
    } else {
      allSources = new AssetSource[1 + sources.length];
      allSources[0] = source;
      System.arraycopy(sources, 0, allSources, 1, sources.length);
    }
    return assets(pattern, new AssetHandler(allSources));
  }

  /**
   * Add a static resource handler.
   *
   * @param pattern Path pattern.
   * @param handler Asset handler.
   * @return A route.
   */
  default @NonNull AssetHandler assets(@NonNull String pattern, @NonNull AssetHandler handler) {
    route(GET, pattern, handler);
    return handler;
  }

  /**
   * Add a route.
   *
   * @param method HTTP method.
   * @param pattern Path pattern.
   * @param handler Application code.
   * @return A route.
   */
  @NonNull Route route(@NonNull String method, @NonNull String pattern, @NonNull Route.Handler handler);

  /**
   * Find a matching route using the given context.
   *
   * <p>If no match exists this method returns a route with a <code>404</code> handler. See {@link
   * Route#NOT_FOUND}.
   *
   * @param ctx Web Context.
   * @return A route match result.
   */
  @NonNull Match match(@NonNull Context ctx);

  /**
   * Find a matching route using the given context.
   *
   * <p>If no match exists this method returns a route with a <code>404</code> handler. See {@link
   * Route#NOT_FOUND}.
   *
   * @param pattern Pattern to match.
   * @param path Path to match.
   * @return A route match result.
   */
  boolean match(@NonNull String pattern, @NonNull String path);

  /* Error handler: */

  /**
   * Map an exception type to a status code.
   *
   * @param type Exception type.
   * @param statusCode Status code.
   * @return This router.
   */
  @NonNull Router errorCode(@NonNull Class<? extends Throwable> type, @NonNull StatusCode statusCode);

  /**
   * Computes the status code for the given exception.
   *
   * @param cause Exception.
   * @return Status code.
   */
  @NonNull StatusCode errorCode(@NonNull Throwable cause);

  /**
   * Add a custom error handler that matches the given status code.
   *
   * @param statusCode Status code.
   * @param handler Error handler.
   * @return This router.
   */
  @NonNull default Router error(@NonNull StatusCode statusCode, @NonNull ErrorHandler handler) {
    return error(statusCode::equals, handler);
  }

  /**
   * Add a custom error handler that matches the given exception type.
   *
   * @param type Exception type.
   * @param handler Error handler.
   * @return This router.
   */
  @NonNull default Router error(@NonNull Class<? extends Throwable> type, @NonNull ErrorHandler handler) {
    return error(
        (ctx, x, statusCode) -> {
          if (type.isInstance(x) || type.isInstance(x.getCause())) {
            handler.apply(ctx, x, statusCode);
          }
        });
  }

  /**
   * Add a custom error handler that matches the given predicate.
   *
   * @param predicate Status code filter.
   * @param handler Error handler.
   * @return This router.
   */
  @NonNull default Router error(@NonNull Predicate<StatusCode> predicate, @NonNull ErrorHandler handler) {
    return error(
        (ctx, x, statusCode) -> {
          if (predicate.test(statusCode)) {
            handler.apply(ctx, x, statusCode);
          }
        });
  }

  /**
   * Add a custom error handler.
   *
   * @param handler Error handler.
   * @return This router.
   */
  @NonNull Router error(@NonNull ErrorHandler handler);

  /**
   * Get the error handler.
   *
   * @return An error handler.
   */
  @NonNull ErrorHandler getErrorHandler();

  /**
   * Application logger.
   *
   * @return Application logger.
   */
  @NonNull Logger getLog();

  /**
   * Router options.
   *
   * @return Router options.
   */
  @NonNull Set<RouterOption> getRouterOptions();

  /**
   * Set router options.
   *
   * @param options router options.
   * @return This router.
   */
  @NonNull Router setRouterOptions(@NonNull RouterOption... options);

  /**
   * Session store. Default use a cookie ID with a memory storage.
   *
   * <p>See {@link SessionStore#memory()}.
   *
   * @return Session store.
   */
  @NonNull SessionStore getSessionStore();

  /**
   * Set session store.
   *
   * @param store Session store.
   * @return This router.
   */
  @NonNull Router setSessionStore(@NonNull SessionStore store);

  /**
   * Get an executor from application registry.
   *
   * @param name Executor name.
   * @return Executor.
   */
  default @NonNull Executor executor(@NonNull String name) {
    return require(Executor.class, name);
  }

  /**
   * Put an executor into the application registry.
   *
   * @param name Executor's name.
   * @param executor Executor.
   * @return This router.
   */
  @NonNull Router executor(@NonNull String name, @NonNull Executor executor);

  /**
   * Template for the flash cookie. Default name is: <code>jooby.flash</code>.
   *
   * @return Template for the flash cookie.
   */
  @NonNull Cookie getFlashCookie();

  /**
   * Sets a cookie used as a template to generate the flash cookie, allowing to customize the cookie
   * name and other cookie parameters.
   *
   * @param flashCookie The cookie template.
   * @return This router.
   */
  @NonNull Router setFlashCookie(@NonNull Cookie flashCookie);

  @NonNull ValueFactory getValueFactory();

  @NonNull Router setValueFactory(@NonNull ValueFactory valueFactory);

  /**
   * Ensure path start with a <code>/</code>(leading slash).
   *
   * @param path Path to process.
   * @return Path with leading slash.
   */
  static @NonNull String leadingSlash(@Nullable String path) {
    if (path == null || path.length() == 0 || path.equals("/")) {
      return "/";
    }
    return path.charAt(0) == '/' ? path : "/" + path;
  }

  /**
   * Strip trailing slashes.
   *
   * @param path Path to process.
   * @return Path without trailing slashes.
   */
  static @NonNull String noTrailingSlash(@NonNull String path) {
    StringBuilder buff = new StringBuilder(path);
    int i = buff.length() - 1;
    while (i > 0 && buff.charAt(i) == '/') {
      buff.setLength(i);
      i -= 1;
    }
    if (path.length() != buff.length()) {
      return buff.toString();
    }
    return path;
  }

  /**
   * Normalize a path by removing consecutive <code>/</code>(slashes).
   *
   * @param path Path to process.
   * @return Safe path pattern.
   */
  static @NonNull String normalizePath(@Nullable String path) {
    if (path == null || path.length() == 0 || path.equals("/")) {
      return "/";
    }
    int len = path.length();
    boolean modified = false;
    int p = 0;
    char[] buff = new char[len + 1];
    if (path.charAt(0) != '/') {
      buff[p++] = '/';
      modified = true;
    }
    for (int i = 0; i < path.length(); i++) {
      char ch = path.charAt(i);
      if (ch != '/') {
        buff[p++] = ch;
      } else if (i == 0 || path.charAt(i - 1) != '/') {
        buff[p++] = ch;
      } else {
        // double slash
        modified = true;
      }
    }
    // creates string?
    return modified ? new String(buff, 0, p) : path;
  }

  /**
   * Extract path keys from given path pattern. A path key (a.k.a path variable) looks like:
   *
   * <pre>/product/{id}</pre>
   *
   * @param pattern Path pattern.
   * @return Path keys.
   */
  static @NonNull List<String> pathKeys(@NonNull String pattern) {
    return pathKeys(pattern, (k, v) -> {});
  }

  /**
   * Extract path keys from given path pattern. A path key (a.k.a path variable) looks like:
   *
   * <pre>/product/{id}</pre>
   *
   * @param pattern Path pattern.
   * @param consumer Listen for key and regex variables found.
   * @return Path keys.
   */
  static @NonNull List<String> pathKeys(
      @NonNull String pattern, BiConsumer<String, String> consumer) {
    List<String> result = new ArrayList<>();
    int start = -1;
    int end = Integer.MAX_VALUE;
    int len = pattern.length();
    int curly = 0;
    for (int i = 0; i < len; i++) {
      char ch = pattern.charAt(i);
      if (ch == '{') {
        if (curly == 0) {
          start = i + 1;
          end = Integer.MAX_VALUE;
        }
        curly += 1;
      } else if (ch == ':') {
        end = i;
      } else if (ch == '}') {
        curly -= 1;
        if (curly == 0) {
          String id = pattern.substring(start, Math.min(i, end));
          String value;
          if (end == Integer.MAX_VALUE) {
            value = null;
          } else {
            value = pattern.substring(end + 1, i);
          }
          consumer.accept(id, value);
          result.add(id);
          start = -1;
          end = Integer.MAX_VALUE;
        }
      } else if (ch == '*') {
        String id;
        if (i == len - 1) {
          id = "*";
        } else {
          id = pattern.substring(i + 1);
        }
        result.add(id);
        consumer.accept(id, "\\.*");
        i = len;
      }
    }
    return switch (result.size()) {
      case 0 -> Collections.emptyList();
      case 1 -> Collections.singletonList(result.get(0));
      default -> unmodifiableList(result);
    };
  }

  /**
   * Look for optional path parameter and expand the given pattern into multiple pattern.
   *
   * <pre>
   *   /path =&gt; [/path]
   *   /{id} =&gt; [/{id}]
   *   /path/{id} =&gt; [/path/{id}]
   *
   *   /{id}? =&gt; [/, /{id}]
   *   /path/{id}? =&gt; [/path, /path/{id}]
   *   /path/{id}/{start}?/{end}? =&gt; [/path/{id}, /path/{id}/{start}, /path/{id}/{start}/{end}]
   *   /path/{id}?/suffix =&gt; [/path, /path/{id}, /path/suffix]
   * </pre>
   *
   * @param pattern Pattern.
   * @return One or more patterns.
   */
  static @NonNull List<String> expandOptionalVariables(@NonNull String pattern) {
    if (pattern == null || pattern.isEmpty() || pattern.equals("/")) {
      return Collections.singletonList("/");
    }
    int len = pattern.length();
    AtomicInteger key = new AtomicInteger();
    Map<Integer, StringBuilder> paths = new HashMap<>();
    BiConsumer<Integer, StringBuilder> pathAppender =
        (index, segment) -> {
          for (int i = index; i < index - 1; i++) {
            paths.get(i).append(segment);
          }
          paths
              .computeIfAbsent(
                  index,
                  current -> {
                    StringBuilder value = new StringBuilder();
                    if (current > 0) {
                      StringBuilder previous = paths.get(current - 1);
                      if (!previous.toString().equals("/")) {
                        value.append(previous);
                      }
                    }
                    return value;
                  })
              .append(segment);
        };
    StringBuilder segment = new StringBuilder();
    boolean isLastOptional = false;
    for (int i = 0; i < len; ) {
      char ch = pattern.charAt(i);
      if (ch == '/') {
        if (segment.length() > 0) {
          pathAppender.accept(key.get(), segment);
          segment.setLength(0);
        }
        segment.append(ch);
        i += 1;
      } else if (ch == '{') {
        segment.append(ch);
        int curly = 1;
        int j = i + 1;
        while (j < len) {
          char next = pattern.charAt(j++);
          segment.append(next);
          if (next == '{') {
            curly += 1;
          } else if (next == '}') {
            curly -= 1;
            if (curly == 0) {
              break;
            }
          }
        }
        if (j < len && pattern.charAt(j) == '?') {
          j += 1;
          isLastOptional = true;
          if (paths.isEmpty()) {
            paths.put(0, new StringBuilder("/"));
          }
          pathAppender.accept(key.incrementAndGet(), segment);
        } else {
          isLastOptional = false;
          pathAppender.accept(key.get(), segment);
        }
        segment.setLength(0);
        i = j;
      } else {
        segment.append(ch);
        i += 1;
      }
    }
    if (paths.isEmpty()) {
      return Collections.singletonList(pattern);
    }
    if (!segment.isEmpty()) {
      pathAppender.accept(key.get(), segment);
      if (isLastOptional) {
        paths.put(key.incrementAndGet(), segment);
      }
    }
    return paths.values().stream().map(StringBuilder::toString).collect(Collectors.toList());
  }

  /**
   * Recreate a path pattern using the given variables. Variable replacement is done using the
   * current index.
   *
   * @param pattern Path pattern.
   * @param values Path keys.
   * @return Path.
   */
  static @NonNull String reverse(@NonNull String pattern, @NonNull Object... values) {
    Map<String, Object> keys = new HashMap<>();
    IntStream.range(0, values.length).forEach(k -> keys.put(Integer.toString(k), values[k]));
    return reverse(pattern, keys);
  }

  /**
   * Recreate a path pattern using the given variables.
   *
   * @param pattern Path pattern.
   * @param keys Path keys.
   * @return Path.
   */
  static @NonNull String reverse(@NonNull String pattern, @NonNull Map<String, Object> keys) {
    StringBuilder path = new StringBuilder();
    int start = 0;
    int end = Integer.MAX_VALUE;
    int len = pattern.length();
    int keyIdx = 0;
    for (int i = 0; i < len; i++) {
      char ch = pattern.charAt(i);
      if (ch == '{') {
        path.append(pattern, start, i);
        start = i + 1;
        end = Integer.MAX_VALUE;
      } else if (ch == ':') {
        end = i;
      } else if (ch == '}') {
        String id = pattern.substring(start, Math.min(i, end));
        Object value = keys.getOrDefault(id, keys.get(Integer.toString(keyIdx++)));
        requireNonNull(value, "Missing key: '" + id + "'");
        path.append(value);
        start = i + 1;
        end = Integer.MAX_VALUE;
      } else if (ch == '*') {
        path.append(pattern, start, i);
        String id;
        if (i == len - 1) {
          id = "*";
        } else {
          id = pattern.substring(i + 1);
        }
        Object value = keys.getOrDefault(id, keys.get(Integer.toString(keyIdx++)));
        requireNonNull(value, "Missing key: '" + id + "'");
        path.append(value);
        start = len;
        i = len;
      }
    }
    if (path.isEmpty()) {
      return pattern;
    }
    if (start > 0) {
      path.append(pattern, start, len);
    }
    return path.toString();
  }
}
