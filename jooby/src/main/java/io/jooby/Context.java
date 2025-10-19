/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.security.cert.Certificate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.internal.LocaleUtils;
import io.jooby.internal.ReadOnlyContext;
import io.jooby.internal.WebSocketSender;
import io.jooby.output.Output;
import io.jooby.output.OutputFactory;
import io.jooby.value.Value;
import io.jooby.value.ValueFactory;

/**
 * HTTP context allows you to interact with the HTTP Request and manipulate the HTTP Response.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface Context extends Registry {

  /** Select an application base on context path prefix matching a provided path. */
  interface Selector {
    /**
     * Select an application base on context path prefix matching a provided path.
     *
     * @param path Path to match.
     * @return Best match application.
     */
    Jooby select(String path);

    static Selector create(List<Jooby> applications) {
      return applications.size() == 1 ? single(applications.getFirst()) : multiple(applications);
    }

    /**
     * Select an application the best match a given path. If none matches it returns the application
     * that has no context path <code>/</code> or the first of the list.
     *
     * @return Best match application.
     */
    private static Selector multiple(List<Jooby> applications) {
      return path -> {
        var defaultApp = applications.getFirst();
        for (var app : applications) {
          var contextPath = app.getContextPath();
          if ("/".equals(contextPath)) {
            defaultApp = app;
          } else if (path.startsWith(contextPath)) {
            return app;
          }
        }
        return defaultApp;
      };
    }

    private static Selector single(Jooby defaultApp) {
      return path -> defaultApp;
    }
  }

  /** Constant for default HTTP port. */
  int PORT = 80;

  /** Constant for default HTTPS port. */
  int SECURE_PORT = 443;

  /** Constant for <code>Accept</code> header. */
  String ACCEPT = "Accept";

  /** Constant for GMT. */
  ZoneId GMT = ZoneId.of("GMT");

  /** RFC1123 date pattern. */
  String RFC1123_PATTERN = "EEE, dd MMM yyyy HH:mm:ss z";

  /** RFC1123 date formatter. */
  DateTimeFormatter RFC1123 = DateTimeFormatter.ofPattern(RFC1123_PATTERN, Locale.US).withZone(GMT);

  /*
   * **********************************************************************************************
   * **** Native methods *************************************************************************
   * **********************************************************************************************
   */

  /**
   * Context attributes (a.k.a request attributes).
   *
   * @return Mutable Context attributes.
   */
  Map<String, Object> getAttributes();

  /**
   * Get an attribute by his key. This is just an utility method around {@link #getAttributes()}.
   * This method look first in current context and fallback to application attributes.
   *
   * @param key Attribute key.
   * @param <T> Attribute type.
   * @return Attribute value or <code>null</code>.
   */
  @Nullable <T> T getAttribute(@NonNull String key);

  /**
   * Set an application attribute.
   *
   * @param key Attribute key.
   * @param value Attribute value.
   * @return This router.
   */
  Context setAttribute(@NonNull String key, Object value);

  /**
   * Get the HTTP router (usually this represents an instance of {@link Jooby}.
   *
   * @return HTTP router (usually this represents an instance of {@link Jooby}.
   */
  Router getRouter();

  OutputFactory getOutputFactory();

  /**
   * Forward executing to another route. We use the given path to find a matching route.
   *
   * <p>NOTE: the entire handler pipeline is executed (filter, decorator, etc.).
   *
   * @param path Path to forward the request.
   * @return Forward result.
   */
  Object forward(@NonNull String path);

  /*
   * **********************************************************************************************
   * **** Request methods *************************************************************************
   * **********************************************************************************************
   */

  /**
   * Flash map.
   *
   * @return Flash map.
   */
  FlashMap flash();

  /**
   * Flash map or null when no flash cookie exists.
   *
   * @return Flash map or null when no flash cookie exists.
   */
  @Nullable FlashMap flashOrNull();

  ValueFactory getValueFactory();

  /**
   * Get a flash attribute.
   *
   * @param name Attribute's name.
   * @return Flash attribute.
   */
  Value flash(@NonNull String name);

  /**
   * Get a flash attribute.
   *
   * @param name Attribute's name.
   * @param defaultValue Default's value. Value won't be persisted to flash context.
   * @return Flash attribute.
   */
  Value flash(@NonNull String name, @NonNull String defaultValue);

  /**
   * Find a session or creates a new session.
   *
   * @return Session.
   */
  Session session();

  /**
   * Find a session attribute using the given name. If there is no session or attribute under that
   * name a missing value is returned.
   *
   * @param name Attribute's name.
   * @return Session's attribute or missing.
   */
  Value session(@NonNull String name);

  /**
   * Find a session attribute using the given name. If there is no session or attribute under that
   * name a missing value is returned.
   *
   * @param name Attribute's name.
   * @param defaultValue Default's value. Value won't be persisted to session context.
   * @return Session's attribute or missing.
   */
  Value session(@NonNull String name, @NonNull String defaultValue);

  /**
   * Find an existing session.
   *
   * @return Existing session or <code>null</code>.
   */
  @Nullable Session sessionOrNull();

  /**
   * Get a cookie matching the given name.
   *
   * @param name Cookie's name.
   * @return Cookie value.
   */
  Value cookie(@NonNull String name);

  /**
   * Get a cookie matching the given name.
   *
   * @param name Cookie's name.
   * @param defaultValue Default's value. Value won't be persisted to response context.
   * @return Cookie value.
   */
  Value cookie(@NonNull String name, @NonNull String defaultValue);

  /**
   * Request cookies.
   *
   * @return Request cookies.
   */
  Map<String, String> cookieMap();

  /**
   * HTTP method in upper-case form.
   *
   * @return HTTP method in upper-case form.
   */
  String getMethod();

  /**
   * Set HTTP method in upper-case form.
   *
   * @param method HTTP method in upper-case form.
   * @return This context.
   */
  Context setMethod(@NonNull String method);

  /**
   * Matching route.
   *
   * @return Matching route.
   */
  Route getRoute();

  /**
   * Check if the request path matches the given pattern.
   *
   * @param pattern Pattern to use.
   * @return True if request path matches the pattern.
   */
  boolean matches(@NonNull String pattern);

  /**
   * Set matching route. This is part of public API, but shouldn't be use by application code.
   *
   * @param route Matching route.
   * @return This context.
   */
  Context setRoute(@NonNull Route route);

  /**
   * Get application context path (a.k.a as base path).
   *
   * @return Application context path (a.k.a as base path).
   */
  default String getContextPath() {
    return getRouter().getContextPath();
  }

  /**
   * Request path without decoding (a.k.a raw Path) without query string.
   *
   * @return Request path without decoding (a.k.a raw Path) without query string.
   */
  String getRequestPath();

  /**
   * Set request path. This is usually done by Web Server or framework, but by user.
   *
   * @param path Request path.
   * @return This context.
   */
  Context setRequestPath(@NonNull String path);

  /**
   * Path variable. Value is decoded.
   *
   * @param name Path key.
   * @return Associated value or a missing value, but never a <code>null</code> reference.
   */
  Value path(@NonNull String name);

  /**
   * Convert the {@link #pathMap()} to the given type.
   *
   * @param type Target type.
   * @param <T> Target type.
   * @return Instance of target type.
   */
  <T> T path(@NonNull Class<T> type);

  /**
   * Convert {@link #pathMap()} to a {@link Value} object.
   *
   * @return A value object.
   */
  Value path();

  /**
   * Path map represent all the path keys with their values.
   *
   * <pre>{@code
   * {
   *   get("/:id", ctx -> ctx.pathMap());
   * }
   * }</pre>
   *
   * A call to:
   *
   * <pre>GET /678</pre>
   *
   * Produces a path map like: <code>id: 678</code>
   *
   * @return Path map from path pattern.
   */
  Map<String, String> pathMap();

  /**
   * Set path map. This method is part of public API but shouldn't be use it by application code.
   *
   * @param pathMap Path map.
   * @return This context.
   */
  Context setPathMap(@NonNull Map<String, String> pathMap);

  /* **********************************************************************************************
   * Query String API
   * **********************************************************************************************
   */

  /**
   * Query string as {@link Value} object.
   *
   * @return Query string as {@link Value} object.
   */
  QueryString query();

  /**
   * Get a query parameter that matches the given name.
   *
   * <pre>{@code
   * {
   *   get("/search", ctx -> {
   *     String q = ctx.query("q").value();
   *     ...
   *   });
   *
   * }
   * }</pre>
   *
   * @param name Parameter name.
   * @return A query value.
   */
  Value query(@NonNull String name);

  /**
   * Get a query parameter that matches the given name.
   *
   * <pre>{@code
   * {
   *   get("/search", ctx -> {
   *     String q = ctx.query("q").value();
   *     ...
   *   });
   *
   * }
   * }</pre>
   *
   * @param name Parameter name.
   * @param defaultValue Default value.
   * @return A query value.
   */
  Value query(@NonNull String name, @NonNull String defaultValue);

  /**
   * Query string with the leading <code>?</code> or empty string. This is the raw query string,
   * without decoding it.
   *
   * @return Query string with the leading <code>?</code> or empty string. This is the raw query
   *     string, without decoding it.
   */
  String queryString();

  /**
   * Convert the queryString to the given type.
   *
   * @param type Target type.
   * @param <T> Target type.
   * @return Query string converted to target type.
   */
  <T> T query(@NonNull Class<T> type);

  /**
   * Query string as simple map.
   *
   * <pre>{@code /search?q=jooby&sort=name}</pre>
   *
   * Produces
   *
   * <pre>{q: jooby, sort: name}</pre>
   *
   * @return Query string as map.
   */
  Map<String, String> queryMap();

  /* **********************************************************************************************
   * Header API
   * **********************************************************************************************
   */

  /**
   * Request headers as {@link Value}.
   *
   * @return Request headers as {@link Value}.
   */
  Value header();

  /**
   * Get a header that matches the given name.
   *
   * @param name Header name. Case insensitive.
   * @return A header value or missing value, never a <code>null</code> reference.
   */
  Value header(@NonNull String name);

  /**
   * Get a header that matches the given name.
   *
   * @param name Header name. Case insensitive.
   * @param defaultValue Default value.
   * @return A header value or missing value, never a <code>null</code> reference.
   */
  Value header(@NonNull String name, @NonNull String defaultValue);

  /**
   * Header as single-value map.
   *
   * @return Header as single-value map, with case insensitive keys.
   */
  Map<String, String> headerMap();

  /**
   * True if the given type matches the `Accept` header. This method returns <code>true</code> if
   * there is no accept header.
   *
   * @param contentType Content type to match.
   * @return True for matching type or if content header is absent.
   */
  boolean accept(@NonNull MediaType contentType);

  /**
   * Check if the accept type list matches the given produces list and return the most specific
   * media type from produces list.
   *
   * @param produceTypes Produced types.
   * @return The most specific produces type.
   */
  @Nullable MediaType accept(@NonNull List<MediaType> produceTypes);

  /**
   * Request <code>Content-Type</code> header or <code>null</code> when missing.
   *
   * @return Request <code>Content-Type</code> header or <code>null</code> when missing.
   */
  @Nullable MediaType getRequestType();

  /**
   * Test whenever this is a CORS preflight request.
   *
   * @return Test whenever this is a CORS preflight request.
   */
  default boolean isPreflight() {
    return getMethod().equals(Router.OPTIONS)
        && !header("Access-Control-Request-Method").isMissing();
  }

  /**
   * Request <code>Content-Type</code> header or <code>null</code> when missing.
   *
   * @param defaults Default content type to use when the header is missing.
   * @return Request <code>Content-Type</code> header or <code>null</code> when missing.
   */
  MediaType getRequestType(MediaType defaults);

  /**
   * Request <code>Content-Length</code> header or <code>-1</code> when missing.
   *
   * @return Request <code>Content-Length</code> header or <code>-1</code> when missing.
   */
  long getRequestLength();

  /**
   * Returns a list of locales that best matches the current request.
   *
   * <p>The first filter argument is the value of <code>Accept-Language</code> as a list of {@link
   * Locale.LanguageRange} instances while the second argument is a list of supported locales
   * specified by {@link Jooby#setLocales(List)} or by the <code>application.lang</code>
   * configuration property.
   *
   * <p>The next example returns a list of matching {@code Locale} instances using the filtering
   * mechanism defined in RFC 4647:
   *
   * <pre>{@code
   * ctx.locales(Locale::filter)
   * }</pre>
   *
   * @param filter A locale filter.
   * @return A list of matching locales.
   */
  default List<Locale> locales(
      BiFunction<List<Locale.LanguageRange>, List<Locale>, List<Locale>> filter) {
    return filter.apply(
        header("Accept-Language")
            .toOptional()
            .flatMap(LocaleUtils::parseRanges)
            .orElseGet(Collections::emptyList),
        getRouter().getLocales());
  }

  /**
   * Returns a list of locales that best matches the current request as per {@link Locale#filter}.
   *
   * @return A list of matching locales or empty list.
   * @see #locales(BiFunction)
   */
  default List<Locale> locales() {
    return locales(Locale::filter);
  }

  /**
   * Returns a locale that best matches the current request.
   *
   * <p>The first filter argument is the value of <code>Accept-Language</code> as a list of {@link
   * Locale.LanguageRange} instances while the second argument is a list of supported locales
   * specified by {@link Jooby#setLocales(List)} or by the <code>application.lang</code>
   * configuration property.
   *
   * <p>The next example returns a {@code Locale} instance for the best-matching language tag using
   * the lookup mechanism defined in RFC 4647.
   *
   * <pre>{@code
   * ctx.locale(Locale::lookup)
   * }</pre>
   *
   * @param filter A locale filter.
   * @return A matching locale.
   */
  default Locale locale(BiFunction<List<Locale.LanguageRange>, List<Locale>, Locale> filter) {
    return filter.apply(
        header("Accept-Language")
            .toOptional()
            .flatMap(LocaleUtils::parseRanges)
            .orElseGet(Collections::emptyList),
        getRouter().getLocales());
  }

  /**
   * Returns a locale that best matches the current request or the default locale specified by
   * {@link Jooby#setLocales(List)} or by the <code>application.lang</code> configuration property.
   *
   * @return A matching locale.
   */
  default Locale locale() {
    return locale(
        (priorityList, locales) ->
            Optional.ofNullable(Locale.lookup(priorityList, locales)).orElse(locales.get(0)));
  }

  /**
   * Current user or <code>null</code> if none was set.
   *
   * @param <T> User type.
   * @return Current user or <code>null</code> if none was set.
   */
  @Nullable <T> T getUser();

  /**
   * Set current user.
   *
   * @param user Current user.
   * @return This context.
   */
  Context setUser(@Nullable Object user);

  /**
   * Recreates full/entire url of the current request using the <code>Host</code> header.
   *
   * <p>If you run behind a reverse proxy that has been configured to send the X-Forwarded-* header,
   * please consider to set {@link Router#setRouterOptions(RouterOptions)} {@link
   * RouterOptions#setTrustProxy(boolean)} option.
   *
   * @return Full/entire request url using the <code>Host</code> header.
   */
  String getRequestURL();

  /**
   * Recreates full/entire request url using the <code>Host</code> header with a custom path/suffix.
   *
   * <p>If you run behind a reverse proxy that has been configured to send the X-Forwarded-* header,
   * please consider to set {@link Router#setRouterOptions(RouterOptions)} {@link
   * RouterOptions#setTrustProxy(boolean)} option.
   *
   * @param path Path or suffix to use, can also include query string parameters.
   * @return Full/entire request url using the <code>Host</code> header.
   */
  String getRequestURL(@NonNull String path);

  /**
   * The IP address of the client or last proxy that sent the request.
   *
   * <p>If you run behind a reverse proxy that has been configured to send the X-Forwarded-* header,
   * please consider to set {@link Router#setRouterOptions(RouterOptions)} {@link
   * RouterOptions#setTrustProxy(boolean)} option.
   *
   * @return The IP address of the client or last proxy that sent the request or <code>empty string
   *     </code> for interrupted requests.
   */
  String getRemoteAddress();

  /**
   * Set IP address of client or last proxy that sent the request.
   *
   * @param remoteAddress Remote Address.
   * @return This context.
   */
  Context setRemoteAddress(@NonNull String remoteAddress);

  /**
   * Return the host that this request was sent to, in general this will be the value of the Host
   * header, minus the port specifier. Unless, it is set manually using the {@link #setHost(String)}
   * method.
   *
   * <p>If you run behind a reverse proxy that has been configured to send the X-Forwarded-* header,
   * please consider to set {@link Router#setRouterOptions(RouterOptions)} {@link
   * RouterOptions#setTrustProxy(boolean)} option.
   *
   * @return Return the host that this request was sent to, in general this will be the value of the
   *     Host header, minus the port specifier.
   */
  String getHost();

  /**
   * Set the host (without the port value).
   *
   * <p>Please keep in mind this method doesn't alter/modify the <code>host</code> header.
   *
   * @param host Host value.
   * @return This context.
   */
  Context setHost(@NonNull String host);

  /**
   * Return the host and port that this request was sent to, in general this will be the value of
   * the Host.
   *
   * <p>If you run behind a reverse proxy that has been configured to send the X-Forwarded-* header,
   * please consider to set {@link Router#setRouterOptions(RouterOptions)} {@link
   * RouterOptions#setTrustProxy(boolean)} option.
   *
   * @return Return the host that this request was sent to, in general this will be the value of the
   *     Host header.
   */
  String getHostAndPort();

  /**
   * Return the port that this request was sent to. In general this will be the value of the Host
   * header, minus the host name.
   *
   * <p>If no host header is present, this method returns the value of {@link #getServerPort()}.
   *
   * @return Return the port that this request was sent to. In general this will be the value of the
   *     Host header, minus the host name.
   */
  int getPort();

  /**
   * Set port this request was sent to.
   *
   * @param port Port.
   * @return This context.
   */
  Context setPort(int port);

  /**
   * The name of the protocol the request. Always in lower-case.
   *
   * @return The name of the protocol the request. Always in lower-case.
   */
  String getProtocol();

  /**
   * The certificates presented by the client for mutual TLS. Empty if ssl is not enabled, or client
   * authentication is not required.
   *
   * @return The certificates presented by the client for mutual TLS. Empty if ssl is not enabled,
   *     or client authentication is not required.
   */
  List<Certificate> getClientCertificates();

  /**
   * Server port for current request.
   *
   * @return Server port for current request.
   */
  int getServerPort();

  /**
   * Server host.
   *
   * @return Server host.
   */
  String getServerHost();

  /**
   * Returns a boolean indicating whether this request was made using a secure channel, such as
   * HTTPS.
   *
   * @return a boolean indicating if the request was made using a secure channel
   */
  boolean isSecure();

  /**
   * HTTP scheme in lower case.
   *
   * @return HTTP scheme in lower case.
   */
  String getScheme();

  /**
   * Set HTTP scheme in lower case.
   *
   * @param scheme HTTP scheme in lower case.
   * @return This context.
   */
  Context setScheme(@NonNull String scheme);

  /* **********************************************************************************************
   * Form/Multipart API
   * **********************************************************************************************
   */

  /**
   * Get form data.
   *
   * <p>Only for <code>application/x-www-form-urlencoded</code> or <code>multipart/form-data</code>
   * request.
   *
   * @return Multipart value.
   */
  Formdata form();

  /**
   * Get a form field that matches the given name.
   *
   * <p>File upload retrieval is available using {@link Context#file(String)}.
   *
   * <p>Only for <code>multipart/form-data</code> request.
   *
   * @param name Field name.
   * @return Multipart value.
   */
  Value form(@NonNull String name);

  /**
   * Convert form data to the given type.
   *
   * <p>Only for <code>application/x-www-form-urlencoded</code> or <code>multipart/form-data</code>
   * request.
   *
   * @param type Target type.
   * @param <T> Target type.
   * @return Target value.
   */
  <T> T form(@NonNull Class<T> type);

  /**
   * Form data as single-value map.
   *
   * <p>Only for <code>application/x-www-form-urlencoded</code> or <code>multipart/form-data</code>
   * request.
   *
   * @return Single-value map.
   */
  Map<String, String> formMap();

  /**
   * All file uploads. Only for <code>multipart/form-data</code> request.
   *
   * @return All file uploads.
   */
  List<FileUpload> files();

  /**
   * All file uploads that matches the given field name.
   *
   * <p>Only for <code>multipart/form-data</code> request.
   *
   * @param name Field name. Please note this is the form field name, not the actual file name.
   * @return All file uploads.
   */
  List<FileUpload> files(@NonNull String name);

  /**
   * A file upload that matches the given field name.
   *
   * <p>Only for <code>multipart/form-data</code> request.
   *
   * @param name Field name. Please note this is the form field name, not the actual file name.
   * @return A file upload.
   */
  FileUpload file(@NonNull String name);

  /* **********************************************************************************************
   * Parameter Lookup
   * **********************************************************************************************
   */

  /**
   * Searches for a parameter in the following order: {@link ParamSource#PATH}, {@link
   * ParamSource#QUERY}, {@link ParamSource#FORM} returning the first non-missing {@link Value}, or
   * a 'missing' {@link Value} if none found.
   *
   * @param name The name of the parameter.
   * @return The first non-missing {@link Value} or a {@link Value} representing a missing value if
   *     none found.
   */
  default Value lookup(String name) {
    return lookup(name, ParamSource.PATH, ParamSource.QUERY, ParamSource.FORM);
  }

  /**
   * Searches for a parameter in the specified sources, in the specified order, returning the first
   * non-missing {@link Value}, or a 'missing' {@link Value} if none found.
   *
   * <p>At least one {@link ParamSource} must be specified.
   *
   * @param name The name of the parameter.
   * @param sources Sources to search in.
   * @return The first non-missing {@link Value} or a {@link Value} representing a missing value if
   *     none found.
   * @throws IllegalArgumentException If no {@link ParamSource}s are specified.
   */
  Value lookup(@NonNull String name, ParamSource... sources);

  /**
   * Returns a {@link ParamLookup} instance which is a fluent interface covering the functionality
   * of the {@link #lookup(String, ParamSource...)} method.
   *
   * <pre>{@code
   * Value foo = ctx.lookup()
   *   .inQuery()
   *   .inPath()
   *   .get("foo");
   * }</pre>
   *
   * @return A {@link ParamLookup} instance.
   * @see ParamLookup
   * @see #lookup(String, ParamSource...)
   */
  ParamLookup lookup();

  /* **********************************************************************************************
   * Request Body
   * **********************************************************************************************
   */

  /**
   * HTTP body which provides access to body content.
   *
   * @return HTTP body which provides access to body content.
   */
  Body body();

  /**
   * Convert the HTTP body to the given type.
   *
   * @param type Reified type.
   * @param <T> Conversion type.
   * @return Instance of conversion type.
   */
  <T> T body(@NonNull Class<T> type);

  /**
   * Convert the HTTP body to the given type.
   *
   * @param type Reified type.
   * @param <T> Conversion type.
   * @return Instance of conversion type.
   */
  <T> T body(@NonNull Type type);

  /**
   * Convert the HTTP body to the given type.
   *
   * @param type Generic type.
   * @param contentType Content type to use.
   * @param <T> Conversion type.
   * @return Instance of conversion type.
   */
  <T> T decode(@NonNull Type type, @NonNull MediaType contentType);

  /* **********************************************************************************************
   * Body MessageDecoder
   * **********************************************************************************************
   */

  /**
   * Get a decoder for the given content type or get an {@link StatusCode#UNSUPPORTED_MEDIA_TYPE}.
   *
   * @param contentType Content type.
   * @return MessageDecoder.
   */
  MessageDecoder decoder(@NonNull MediaType contentType);

  /* **********************************************************************************************
   * Dispatch methods
   * **********************************************************************************************
   */

  /**
   * True when request runs in IO threads.
   *
   * @return True when request runs in IO threads.
   */
  boolean isInIoThread();

  /**
   * Dispatch context to a worker threads. Worker threads allow to execute blocking code. The
   * default worker thread pool is provided by web server or by application code using the {@link
   * Jooby#setWorker(Executor)}.
   *
   * <p>Example:
   *
   * <pre>{@code
   * get("/", ctx -> {
   *   return ctx.dispatch(() -> {
   *
   *     // run blocking code
   *
   *   }):
   * });
   *
   * }</pre>
   *
   * @param action Application code.
   * @return This context.
   */
  Context dispatch(@NonNull Runnable action);

  /**
   * Dispatch context to the given executor.
   *
   * <p>Example:
   *
   * <pre>{@code
   * Executor executor = ...;
   * get("/", ctx -> {
   *   return ctx.dispatch(executor, () -> {
   *
   *     // run blocking code
   *
   *   }):
   * });
   *
   * }</pre>
   *
   * @param executor Executor to use.
   * @param action Application code.
   * @return This context.
   */
  Context dispatch(@NonNull Executor executor, @NonNull Runnable action);

  /**
   * Perform a websocket handsahke and upgrade a HTTP GET into a websocket protocol.
   *
   * <p>NOTE: This method is part of Public API, but shouldn't be used by client code.
   *
   * @param handler Web socket initializer.
   * @return This context.
   */
  Context upgrade(@NonNull WebSocket.Initializer handler);

  /**
   * Perform a server-sent event handshake and upgrade HTTP GET into a Server-Sent protocol.
   *
   * <p>NOTE: This method is part of Public API, but shouldn't be used by client code.
   *
   * @param handler Server-Sent event handler.
   * @return This context.
   */
  Context upgrade(@NonNull ServerSentEmitter.Handler handler);

  /*
   * **********************************************************************************************
   * **** Response methods *************************************************************************
   * **********************************************************************************************
   */

  /**
   * Set response header.
   *
   * @param name Header name.
   * @param value Header value.
   * @return This context.
   */
  Context setResponseHeader(@NonNull String name, @NonNull Date value);

  /**
   * Set response header.
   *
   * @param name Header name.
   * @param value Header value.
   * @return This context.
   */
  Context setResponseHeader(@NonNull String name, @NonNull Instant value);

  /**
   * Set response header.
   *
   * @param name Header name.
   * @param value Header value.
   * @return This context.
   */
  Context setResponseHeader(@NonNull String name, @NonNull Object value);

  /**
   * Set response header.
   *
   * @param name Header name.
   * @param value Header value.
   * @return This context.
   */
  Context setResponseHeader(@NonNull String name, @NonNull String value);

  /**
   * Remove a response header.
   *
   * @param name Header's name.
   * @return This context.
   */
  Context removeResponseHeader(@NonNull String name);

  /**
   * Clear/reset all the headers, including cookies.
   *
   * @return This context.
   */
  Context removeResponseHeaders();

  /**
   * Set response content length header.
   *
   * @param length Response length.
   * @return This context.
   */
  Context setResponseLength(long length);

  /**
   * Get response header.
   *
   * @param name Header's name.
   * @return Header's value (if set previously) or <code>null</code>.
   */
  @Nullable String getResponseHeader(@NonNull String name);

  /**
   * Get response content length or <code>-1</code> when none was set.
   *
   * @return Response content length or <code>-1</code> when none was set.
   */
  long getResponseLength();

  /**
   * Set/add a cookie to response.
   *
   * @param cookie Cookie to add.
   * @return This context.
   */
  Context setResponseCookie(@NonNull Cookie cookie);

  /**
   * Set response content type header.
   *
   * @param contentType Content type.
   * @return This context.
   */
  Context setResponseType(@NonNull String contentType);

  /**
   * Set response content type header.
   *
   * @param contentType Content type.
   * @return This context.
   */
  Context setResponseType(@NonNull MediaType contentType);

  /**
   * Set the default response content type header. It is used if the response content type header
   * was not set yet.
   *
   * @param contentType Content type.
   * @return This context.
   */
  Context setDefaultResponseType(@NonNull MediaType contentType);

  /**
   * Get response content type.
   *
   * @return Response content type.
   */
  MediaType getResponseType();

  /**
   * Set response status code.
   *
   * @param statusCode Status code.
   * @return This context.
   */
  Context setResponseCode(@NonNull StatusCode statusCode);

  /**
   * Set response status code.
   *
   * @param statusCode Status code.
   * @return This context.
   */
  Context setResponseCode(int statusCode);

  /**
   * Get response status code.
   *
   * @return Response status code.
   */
  StatusCode getResponseCode();

  /**
   * Render a value and send the response to client.
   *
   * @param value Object value.
   * @return This context.
   */
  Context render(@NonNull Object value);

  /**
   * HTTP response channel as output stream. Usually for chunked responses.
   *
   * @return HTTP channel as output stream. Usually for chunked responses.
   */
  OutputStream responseStream();

  /**
   * HTTP response channel as output stream. Usually for chunked responses.
   *
   * @param contentType Media type.
   * @return HTTP channel as output stream. Usually for chunked responses.
   */
  OutputStream responseStream(@NonNull MediaType contentType);

  /**
   * HTTP response channel as output stream. Usually for chunked responses.
   *
   * @param contentType Content type.
   * @param consumer Output stream consumer.
   * @return HTTP channel as output stream. Usually for chunked responses.
   * @throws Exception Is something goes wrong.
   */
  Context responseStream(
      MediaType contentType, @NonNull SneakyThrows.Consumer<OutputStream> consumer)
      throws Exception;

  /**
   * HTTP response channel as output stream. Usually for chunked responses.
   *
   * @param consumer Output stream consumer.
   * @return HTTP channel as output stream. Usually for chunked responses.
   * @throws Exception Is something goes wrong.
   */
  Context responseStream(@NonNull SneakyThrows.Consumer<OutputStream> consumer) throws Exception;

  /**
   * HTTP response channel as chunker.
   *
   * @return HTTP channel as chunker. Usually for chunked response.
   */
  Sender responseSender();

  /**
   * HTTP response channel as response writer.
   *
   * @return HTTP channel as response writer. Usually for chunked response.
   */
  PrintWriter responseWriter();

  /**
   * HTTP response channel as response writer.
   *
   * @param contentType Content type.
   * @return HTTP channel as response writer. Usually for chunked response.
   */
  PrintWriter responseWriter(@NonNull MediaType contentType);

  /**
   * HTTP response channel as response writer.
   *
   * @param consumer Writer consumer.
   * @return This context.
   * @throws Exception Is something goes wrong.
   */
  Context responseWriter(@NonNull SneakyThrows.Consumer<PrintWriter> consumer) throws Exception;

  /**
   * HTTP response channel as response writer.
   *
   * @param contentType Content type.
   * @param consumer Writer consumer.
   * @return This context.
   * @throws Exception Is something goes wrong.
   */
  Context responseWriter(
      MediaType contentType, @NonNull SneakyThrows.Consumer<PrintWriter> consumer) throws Exception;

  /**
   * Send a <code>302</code> response.
   *
   * @param location Location.
   * @return This context.
   */
  Context sendRedirect(@NonNull String location);

  /**
   * Send a redirect response.
   *
   * @param redirect Redirect status code.
   * @param location Location.
   * @return This context.
   */
  Context sendRedirect(@NonNull StatusCode redirect, @NonNull String location);

  /**
   * Send response data.
   *
   * @param data Response. Use UTF-8 charset.
   * @return This context.
   */
  Context send(@NonNull String data);

  /**
   * Send response data.
   *
   * @param data Response.
   * @param charset Charset.
   * @return This context.
   */
  Context send(@NonNull String data, @NonNull Charset charset);

  /**
   * Send response data.
   *
   * @param data Response.
   * @return This context.
   */
  Context send(@NonNull byte[] data);

  /**
   * Send response data.
   *
   * @param data Response.
   * @return This context.
   */
  Context send(@NonNull ByteBuffer data);

  /**
   * Send response data.
   *
   * @param output Output.
   * @return This context.
   */
  Context send(@NonNull Output output);

  /**
   * Send response data.
   *
   * @param data Response.
   * @return This context.
   */
  Context send(@NonNull byte[]... data);

  /**
   * Send response data.
   *
   * @param data Response.
   * @return This context.
   */
  Context send(@NonNull ByteBuffer[] data);

  /**
   * Send response data.
   *
   * @param channel Response input.
   * @return This context.
   */
  Context send(@NonNull ReadableByteChannel channel);

  /**
   * Send response data.
   *
   * @param input Response.
   * @return This context.
   */
  Context send(@NonNull InputStream input);

  /**
   * Send a file download response.
   *
   * @param file File download.
   * @return This context.
   */
  Context send(@NonNull FileDownload file);

  /**
   * Send a file response.
   *
   * @param file File response.
   * @return This context.
   */
  Context send(@NonNull Path file);

  /**
   * Send a file response.
   *
   * @param file File response.
   * @return This context.
   */
  Context send(@NonNull FileChannel file);

  /**
   * Send an empty response with the given status code.
   *
   * @param statusCode Status code.
   * @return This context.
   */
  Context send(@NonNull StatusCode statusCode);

  /**
   * Send an error response. Status code is computed via {@link Router#errorCode(Throwable)}.
   *
   * @param cause Error. If this is a fatal error it is going to be rethrow it.
   * @return This context.
   */
  Context sendError(@NonNull Throwable cause);

  /**
   * Send an error response.
   *
   * @param cause Error. If this is a fatal error it is going to be rethrow it.
   * @param statusCode Status code.
   * @return This context.
   */
  Context sendError(@NonNull Throwable cause, @NonNull StatusCode statusCode);

  /**
   * True if response already started.
   *
   * @return True if response already started.
   */
  boolean isResponseStarted();

  /**
   * True if response headers are cleared on application error. If none set it uses the
   * default/global value specified by {@link Router#setRouterOptions(RouterOptions)} {@link
   * RouterOptions#setResetHeadersOnError(boolean)} option.
   *
   * @return True if response headers are cleared on application error. If none set it uses the
   *     default/global value specified by {@link Router#setRouterOptions(RouterOptions)} {@link
   *     RouterOptions#setResetHeadersOnError(boolean)} option.
   */
  boolean getResetHeadersOnError();

  /**
   * Set whenever reset/clear headers on application error.
   *
   * @param value True for reset/clear headers.
   * @return This context.
   */
  Context setResetHeadersOnError(boolean value);

  /**
   * Add a complete listener.
   *
   * @param task Task to execute.
   * @return This context.
   */
  Context onComplete(@NonNull Route.Complete task);

  /* **********************************************************************************************
   * Factory methods
   * **********************************************************************************************
   */

  /**
   * Wrap a HTTP context and make read only. Attempt to modify the HTTP response resulted in
   * exception.
   *
   * @param ctx Originating context.
   * @return Read only context.
   */
  static Context readOnly(@NonNull Context ctx) {
    return new ReadOnlyContext(ctx);
  }

  /**
   * Wrap a HTTP context and make it WebSocket friendly. Attempt to modify the HTTP response is
   * completely ignored, except for {@link #send(byte[])} and {@link #send(String)} which are
   * delegated to the given web socket.
   *
   * <p>This context is necessary for creating a bridge between {@link MessageEncoder} and {@link
   * WebSocket}.
   *
   * <p>This method is part of Public API, but direct usage is discouraged.
   *
   * @param ctx Originating context.
   * @param ws WebSocket.
   * @param binary True for sending binary message.
   * @return Read only context.
   */
  static Context websocket(
      Context ctx, WebSocket ws, boolean binary, WebSocket.WriteCallback callback) {
    return new WebSocketSender(ctx, ws, binary, callback);
  }
}
