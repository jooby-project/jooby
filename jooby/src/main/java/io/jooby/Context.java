/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.internal.LocaleUtils;
import io.jooby.internal.ParamLookupImpl;
import io.jooby.internal.ReadOnlyContext;
import io.jooby.internal.WebSocketSender;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;

/**
 * HTTP context allows you to interact with the HTTP Request and manipulate the HTTP Response.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface Context extends Registry {

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
  DateTimeFormatter RFC1123 = DateTimeFormatter
      .ofPattern(RFC1123_PATTERN, Locale.US)
      .withZone(GMT);

  /*
   * **********************************************************************************************
   * **** Native methods *************************************************************************
   * **********************************************************************************************
   */

  /**
   * Context attributes (a.k.a request attributes).
   *
   * @return Context attributes.
   */
  @Nonnull Map<String, Object> getAttributes();

  /**
   * Get an attribute by his key. This is just an utility method around {@link #getAttributes()}.
   * This method look first in current context and fallback to application attributes.
   *
   * @param key Attribute key.
   * @param <T> Attribute type.
   * @return Attribute value.
   */
  @Nonnull <T> T attribute(@Nonnull String key);

  /**
   * Set an application attribute.
   *
   * @param key Attribute key.
   * @param value Attribute value.
   * @return This router.
   */
  @Nonnull Context attribute(@Nonnull String key, Object value);

  /**
   * Get the HTTP router (usually this represent an instance of {@link Jooby}.
   *
   * @return HTTP router (usually this represent an instance of {@link Jooby}.
   */
  @Nonnull Router getRouter();

  /**
   * Forward executing to another route. We use the given path to find a matching route.
   *
   * NOTE: the entire handler pipeline is executed (filter, decorator, etc.).
   *
   * @param path Path to forward the request.
   * @return This context.
   */
  @Nonnull Context forward(@Nonnull String path);

  /**
   * Converts a value (single or hash) into the given type.
   *
   * @param value Value to convert.
   * @param type Expected type.
   * @param <T> Generic type.
   * @return Converted value.
   */
  @Nullable <T> T convert(@Nonnull ValueNode value, @Nonnull Class<T> type);

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
  @Nonnull FlashMap flash();

  /**
   * Get a flash attribute.
   *
   * @param name Attribute's name.
   * @return Flash attribute.
   */
  @Nonnull Value flash(@Nonnull String name);

  /**
   * Find a session or creates a new session.
   *
   * @return Session.
   */
  @Nonnull Session session();

  /**
   * Find a session attribute using the given name. If there is no session or attribute under that
   * name a missing value is returned.
   *
   * @param name Attribute's name.
   * @return Session's attribute or missing.
   */
  @Nonnull Value session(@Nonnull String name);

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
  @Nonnull Value cookie(@Nonnull String name);

  /**
   * Request cookies.
   *
   * @return Request cookies.
   */
  @Nonnull Map<String, String> cookieMap();

  /**
   * HTTP method in upper-case form.
   *
   * @return HTTP method in upper-case form.
   */
  @Nonnull String getMethod();

  /**
   * Set HTTP method in upper-case form.
   *
   * @param method HTTP method in upper-case form.
   * @return This context.
   */
  @Nonnull Context setMethod(@Nonnull String method);

  /**
   * Matching route.
   *
   * @return Matching route.
   */
  @Nonnull Route getRoute();

  /**
   * Check if the request path matches the given pattern.
   *
   * @param pattern Pattern to use.
   * @return True if request path matches the pattern.
   */
  boolean matches(@Nonnull String pattern);

  /**
   * Set matching route. This is part of public API, but shouldn't be use by application code.
   *
   * @param route Matching route.
   * @return This context.
   */
  @Nonnull Context setRoute(@Nonnull Route route);

  /**
   * Get application context path (a.k.a as base path).
   *
   * @return Application context path (a.k.a as base path).
   */
  default @Nonnull String getContextPath() {
    return getRouter().getContextPath();
  }

  /**
   * Request path without decoding (a.k.a raw Path) without query string.
   *
   * @return Request path without decoding (a.k.a raw Path) without query string.
   */
  @Nonnull String getRequestPath();

  /**
   * Set request path. This is usually done by Web Server or framework, but by user.
   *
   * @param path Request path.
   * @return This context.
   */
  @Nonnull Context setRequestPath(@Nonnull String path);

  /**
   * Path variable. Value is decoded.
   *
   * @param name Path key.
   * @return Associated value or a missing value, but never a <code>null</code> reference.
   */
  @Nonnull Value path(@Nonnull String name);

  /**
   * Convert the {@link #pathMap()} to the given type.
   *
   * @param type Target type.
   * @param <T> Target type.
   * @return Instance of target type.
   */
  @Nonnull <T> T path(@Nonnull Class<T> type);

  /**
   * Convert {@link #pathMap()} to a {@link ValueNode} object.
   * @return A value object.
   */
  @Nonnull ValueNode path();

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
   * <pre>GET /678</pre>
   * Produces a path map like: <code>id: 678</code>
   *
   * @return Path map from path pattern.
   */
  @Nonnull Map<String, String> pathMap();

  /**
   * Set path map. This method is part of public API but shouldn't be use it by application code.
   *
   * @param pathMap Path map.
   * @return This context.
   */
  @Nonnull Context setPathMap(@Nonnull Map<String, String> pathMap);

  /* **********************************************************************************************
   * Query String API
   * **********************************************************************************************
   */

  /**
   * Query string as {@link ValueNode} object.
   *
   * @return Query string as {@link ValueNode} object.
   */
  @Nonnull QueryString query();

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
  @Nonnull ValueNode query(@Nonnull String name);

  /**
   * Query string with the leading <code>?</code> or empty string. This is the raw query string,
   * without decoding it.
   *
   * @return Query string with the leading <code>?</code> or empty string. This is the raw query
   *    string, without decoding it.
   */
  @Nonnull String queryString();

  /**
   * Convert the queryString to the given type.
   *
   * @param type Target type.
   * @param <T> Target type.
   * @return Query string converted to target type.
   */
  @Nonnull <T> T query(@Nonnull Class<T> type);

  /**
   * Query string as simple map.
   *
   * <pre>{@code/search?q=jooby&sort=name}</pre>
   *
   * Produces
   *
   * <pre>{q: jooby, sort: name}</pre>
   *
   * @return Query string as map.
   */
  @Nonnull Map<String, String> queryMap();

  /**
   * Query string as multi-value map.
   *
   * <pre>{@code/search?q=jooby&sort=name&sort=id}</pre>
   *
   * Produces
   *
   * <pre>{q: [jooby], sort: [name, id]}</pre>
   *
   * @return Query string as map.
   */
  @Nonnull Map<String, List<String>> queryMultimap();

  /* **********************************************************************************************
   * Header API
   * **********************************************************************************************
   */

  /**
   * Request headers as {@link ValueNode}.
   *
   * @return Request headers as {@link ValueNode}.
   */
  @Nonnull ValueNode header();

  /**
   * Get a header that matches the given name.
   *
   * @param name Header name. Case insensitive.
   * @return A header value or missing value, never a <code>null</code> reference.
   */
  @Nonnull Value header(@Nonnull String name);

  /**
   * Header as single-value map.
   *
   * @return Header as single-value map.
   */
  @Nonnull Map<String, String> headerMap();

  /**
   * Header as multi-value map.
   *
   * @return Header as multi-value map.
   */
  @Nonnull Map<String, List<String>> headerMultimap();

  /**
   * True if the given type matches the `Accept` header. This method returns <code>true</code>
   * if there is no accept header.
   *
   * @param contentType Content type to match.
   * @return True for matching type or if content header is absent.
   */
  boolean accept(@Nonnull MediaType contentType);

  /**
   * Check if the accept type list matches the given produces list and return the most
   * specific media type from produces list.
   *
   * @param produceTypes Produced types.
   * @return The most specific produces type.
   */
  @Nullable MediaType accept(@Nonnull List<MediaType> produceTypes);

  /**
   * Request <code>Content-Type</code> header or <code>null</code> when missing.
   *
   * @return Request <code>Content-Type</code> header or <code>null</code> when missing.
   */
  @Nullable MediaType getRequestType();

  /**
   * Request <code>Content-Type</code> header or <code>null</code> when missing.
   *
   * @param defaults Default content type to use when the header is missing.
   * @return Request <code>Content-Type</code> header or <code>null</code> when missing.
   */
  @Nonnull MediaType getRequestType(MediaType defaults);

  /**
   * Request <code>Content-Length</code> header or <code>-1</code> when missing.
   *
   * @return Request <code>Content-Length</code> header or <code>-1</code> when missing.
   */
  long getRequestLength();

  /**
   * Returns a list of locales that best matches the current request.
   *
   * The first filter argument is the value of <code>Accept-Language</code> as a list of
   * {@link Locale.LanguageRange} instances while the second argument is a list of supported
   * locales specified by {@link Jooby#setLocales(List)} or by the
   * <code>application.lang</code> configuration property.
   *
   * The next example returns a list of matching {@code Locale} instances using the filtering
   * mechanism defined in RFC 4647:
   *
   * <pre>{@code
   * ctx.locales(Locale::filter)
   * }</pre>
   *
   * @param filter A locale filter.
   * @return A list of matching locales.
   */
  @Nonnull default List<Locale> locales(BiFunction<List<Locale.LanguageRange>, List<Locale>, List<Locale>> filter) {
    return filter.apply(header("Accept-Language")
        .toOptional()
        .flatMap(LocaleUtils::parseRanges)
        .orElseGet(Collections::emptyList), getRouter().getLocales());
  }

  /**
   * Returns a list of locales that best matches the current request as per {@link Locale#filter}.
   *
   * @return A list of matching locales or empty list.
   * @see #locales(BiFunction)
   */
  @Nonnull default List<Locale> locales() {
    return locales(Locale::filter);
  }

  /**
   * Returns a locale that best matches the current request.
   *
   * The first filter argument is the value of <code>Accept-Language</code> as a list of
   * {@link Locale.LanguageRange} instances while the second argument is a list of supported
   * locales specified by {@link Jooby#setLocales(List)} or by the
   * <code>application.lang</code> configuration property.
   *
   * The next example returns a {@code Locale} instance for the best-matching language
   * tag using the lookup mechanism defined in RFC 4647.
   *
   * <pre>{@code
   * ctx.locale(Locale::lookup)
   * }</pre>
   *
   * @param filter A locale filter.
   * @return A matching locale.
   */
  @Nonnull default Locale locale(BiFunction<List<Locale.LanguageRange>, List<Locale>, Locale> filter) {
    return filter.apply(header("Accept-Language")
        .toOptional()
        .flatMap(LocaleUtils::parseRanges)
        .orElseGet(Collections::emptyList), getRouter().getLocales());
  }

  /**
   * Returns a locale that best matches the current request or the default locale specified
   * by {@link Jooby#setLocales(List)} or by the <code>application.lang</code>
   * configuration property.
   *
   * @return A matching locale.
   */
  @Nonnull default Locale locale() {
    return locale((priorityList, locales) -> Optional.ofNullable(Locale.lookup(priorityList, locales))
        .orElse(locales.get(0)));
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
  @Nonnull Context setUser(@Nullable Object user);

  /**
   * Recreates full/entire request url using the <code>Host</code> header.
   *
   * If you run behind a reverse proxy that has been configured to send the X-Forwarded-* header,
   * please consider to set {@link Router#setTrustProxy(boolean)} option.
   *
   * @return Full/entire request url using the <code>Host</code> header.
   */
  @Nonnull String getRequestURL();

  /**
   * Recreates full/entire request url using the <code>Host</code> header.
   *
   * If you run behind a reverse proxy that has been configured to send the X-Forwarded-* header,
   * please consider to set {@link Router#setTrustProxy(boolean)} option.
   *
   * @param path Path to use.
   * @return Full/entire request url using the <code>Host</code> header.
   */
  @Nonnull String getRequestURL(@Nonnull String path);

  /**
   * The IP address of the client or last proxy that sent the request.
   *
   * If you run behind a reverse proxy that has been configured to send the X-Forwarded-* header,
   * please consider to set {@link Router#setTrustProxy(boolean)} option.
   *
   * @return The IP address of the client or last proxy that sent the request.
   */
  @Nonnull String getRemoteAddress();

  /**
   * Set IP address of client or last proxy that sent the request.
   *
   * @param remoteAddress Remote Address.
   * @return This context.
   */
  @Nonnull Context setRemoteAddress(@Nonnull String remoteAddress);

  /**
   * Return the host that this request was sent to, in general this will be the
   * value of the Host header, minus the port specifier. Unless, it is set manually using the
   * {@link #setHost(String)} method.
   *
   * If you run behind a reverse proxy that has been configured to send the X-Forwarded-* header,
   * please consider to set {@link Router#setTrustProxy(boolean)} option.
   *
   * @return Return the host that this request was sent to, in general this will be the
   *     value of the Host header, minus the port specifier.
   */
  @Nonnull String getHost();

  /**
   * Set the host (without the port value).
   *
   * Please keep in mind this method doesn't alter/modify the <code>host</code> header.
   *
   * @param host Host value.
   * @return This context.
   */
  @Nonnull Context setHost(@Nonnull String host);

  /**
   * Return the host and port that this request was sent to, in general this will be the
   * value of the Host.
   *
   * If you run behind a reverse proxy that has been configured to send the X-Forwarded-* header,
   * please consider to set {@link Router#setTrustProxy(boolean)} option.
   *
   * @return Return the host that this request was sent to, in general this will be the
   *     value of the Host header.
   */
  @Nonnull String getHostAndPort();

  /**
   * Return the port that this request was sent to. In general this will be the value of the Host
   * header, minus the host name.
   *
   * If no host header is present, this method returns the value of {@link #getServerPort()}.
   *
   * @return Return the port that this request was sent to. In general this will be the value of
   *    the Host header, minus the host name.
   */
  int getPort();

  /**
   * Set port this request was sent to.
   *
   * @param port Port.
   * @return This context.
   */
  @Nonnull Context setPort(int port);

  /**
   * The name of the protocol the request. Always in lower-case.
   *
   * @return The name of the protocol the request. Always in lower-case.
   */
  @Nonnull String getProtocol();

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
  @Nonnull String getServerHost();

  /**
   *
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
  @Nonnull String getScheme();

  /**
   * Set HTTP scheme in lower case.
   *
   * @param scheme HTTP scheme in lower case.
   * @return This context.
   */
  @Nonnull Context setScheme(@Nonnull String scheme);

  /* **********************************************************************************************
   * Form API
   * **********************************************************************************************
   */

  /**
   * Formdata as {@link ValueNode}. This method is for <code>application/form-url-encoded</code>
   * request.
   *
   * @return Formdata as {@link ValueNode}. This method is for <code>application/form-url-encoded</code>
   *    request.
   */
  @Nonnull Formdata form();

  /**
   * Formdata as multi-value map. Only for <code>application/form-url-encoded</code> request.
   *
   * @return Formdata as multi-value map. Only for <code>application/form-url-encoded</code>
   *     request.
   */
  @Nonnull Map<String, List<String>> formMultimap();

  /**
   * Formdata as single-value map. Only for <code>application/form-url-encoded</code> request.
   *
   * @return Formdata as single-value map. Only for <code>application/form-url-encoded</code>
   *     request.
   */
  @Nonnull Map<String, String> formMap();

  /**
   * Form field that matches the given name. Only for <code>application/form-url-encoded</code>
   * request.
   *
   * @param name Field name.
   * @return Form value.
   */
  @Nonnull ValueNode form(@Nonnull String name);

  /**
   * Convert formdata to the given type. Only for <code>application/form-url-encoded</code>
   * request.
   *
   * @param type Target type.
   * @param <T> Target type.
   * @return Formdata as requested type.
   */
  @Nonnull <T> T form(@Nonnull Class<T> type);

  /* **********************************************************************************************
   * Multipart API
   * **********************************************************************************************
   */

  /**
   * Get multipart data. Only for <code>multipart/form-data</code> request..
   *
   * @return Multipart value.
   */
  @Nonnull Multipart multipart();

  /**
   * Get a multipart field that matches the given name.
   *
   * File upload retrieval is available using {@link Context#file(String)}.
   *
   * Only for <code>multipart/form-data</code> request.
   *
   * @param name Field name.
   * @return Multipart value.
   */
  @Nonnull ValueNode multipart(@Nonnull String name);

  /**
   * Convert multipart data to the given type.
   *
   * Only for <code>multipart/form-data</code> request.
   *
   * @param type Target type.
   * @param <T> Target type.
   * @return Target value.
   */
  @Nonnull <T> T multipart(@Nonnull Class<T> type);

  /**
   * Multipart data as multi-value map.
   *
   * Only for <code>multipart/form-data</code> request.
   *
   * @return Multi-value map.
   */
  @Nonnull Map<String, List<String>> multipartMultimap();

  /**
   * Multipart data as single-value map.
   *
   * Only for <code>multipart/form-data</code> request.
   *
   * @return Single-value map.
   */
  @Nonnull Map<String, String> multipartMap();

  /**
   * All file uploads. Only for <code>multipart/form-data</code> request.
   *
   * @return All file uploads.
   */
  @Nonnull List<FileUpload> files();

  /**
   * All file uploads that matches the given field name.
   *
   * Only for <code>multipart/form-data</code> request.
   *
   * @param name Field name. Please note this is the form field name, not the actual file name.
   * @return All file uploads.
   */
  @Nonnull List<FileUpload> files(@Nonnull String name);

  /**
   * A file upload that matches the given field name.
   *
   * Only for <code>multipart/form-data</code> request.
   *
   * @param name Field name. Please note this is the form field name, not the actual file name.
   * @return A file upload.
   */
  @Nonnull FileUpload file(@Nonnull String name);

  /* **********************************************************************************************
   * Parameter Lookup
   * **********************************************************************************************
   */

  /**
   * Searches for a parameter in the specified sources, in the specified
   * order, returning the first non-missing {@link Value}, or a 'missing'
   * {@link Value} if none found.
   * <p>
   * At least one {@link ParamSource} must be specified.
   *
   * @param name The name of the parameter.
   * @param sources Sources to search in.
   * @return The first non-missing {@link Value} or a {@link Value} representing
   * a missing value if none found.
   * @throws IllegalArgumentException If no {@link ParamSource}s are specified.
   */
  default Value lookup(String name, ParamSource... sources) {
    if (sources.length == 0) {
      throw new IllegalArgumentException("No parameter sources were specified.");
    }

    return Arrays.stream(sources)
        .map(source -> source.provider.apply(this, name))
        .filter(value -> !value.isMissing())
        .findFirst()
        .orElseGet(() -> Value.missing(name));
  }

  /**
   * Returns a {@link ParamLookup} instance which is a fluent interface covering
   * the functionality of the {@link #lookup(String, ParamSource...)} method.
   *
   * <pre>{@code
   *  Value foo = ctx.lookup()
   *    .inQuery()
   *    .inPath()
   *    .get("foo");
   * }</pre>
   *
   * @return A {@link ParamLookup} instance.
   * @see ParamLookup
   * @see #lookup(String, ParamSource...)
   */
  default ParamLookup lookup() {
    return new ParamLookupImpl(this);
  }

  /* **********************************************************************************************
   * Request Body
   * **********************************************************************************************
   */

  /**
   * HTTP body which provides access to body content.
   *
   * @return HTTP body which provides access to body content.
   */
  @Nonnull Body body();

  /**
   * Convert the HTTP body to the given type.
   *
   * @param type Reified type.
   * @param <T> Conversion type.
   * @return Instance of conversion type.
   */
  @Nonnull <T> T body(@Nonnull Class<T> type);

  /**
   * Convert the HTTP body to the given type.
   *
   * @param type Reified type.
   * @param <T> Conversion type.
   * @return Instance of conversion type.
   */
  @Nonnull <T> T body(@Nonnull Type type);

  /**
   * Convert the HTTP body to the given type.
   *
   * @param type Generic type.
   * @param contentType Content type to use.
   * @param <T> Conversion type.
   * @return Instance of conversion type.
   */
  @Nonnull <T> T decode(@Nonnull Type type, @Nonnull MediaType contentType);

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
  @Nonnull MessageDecoder decoder(@Nonnull MediaType contentType);

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
   * Dispatch context to a worker threads. Worker threads allow to execute blocking code.
   * The default worker thread pool is provided by web server or by application code using the
   * {@link Jooby#setWorker(Executor)}.
   *
   * Example:
   *
   * <pre>{@code
   *
   *   get("/", ctx -> {
   *     return ctx.dispatch(() -> {
   *
   *       // run blocking code
   *
   *     }):
   *   });
   *
   * }</pre>
   *
   * @param action Application code.
   * @return This context.
   */
  @Nonnull Context dispatch(@Nonnull Runnable action);

  /**
   * Dispatch context to the given executor.
   *
   * Example:
   *
   * <pre>{@code
   *
   *   Executor executor = ...;
   *   get("/", ctx -> {
   *     return ctx.dispatch(executor, () -> {
   *
   *       // run blocking code
   *
   *     }):
   *   });
   *
   * }</pre>
   *
   * @param executor Executor to use.
   * @param action Application code.
   * @return This context.
   */
  @Nonnull Context dispatch(@Nonnull Executor executor, @Nonnull Runnable action);

  /**
   * Tells context that response will be generated form a different thread. This operation is
   * similar to {@link #dispatch(Runnable)} except there is no thread dispatching here.
   *
   * This operation integrates easily with third-party libraries like rxJava or others.
   *
   * @param next Application code.
   * @return This context.
   * @throws Exception When detach operation fails.
   */
  @Nonnull Context detach(@Nonnull Route.Handler next) throws Exception;

  /**
   * Perform a websocket handsahke and upgrade a HTTP GET into a websocket protocol.
   *
   * NOTE: This method is part of Public API, but shouldn't be used by client code.
   *
   * @param handler Web socket initializer.
   * @return This context.
   */
  @Nonnull Context upgrade(@Nonnull WebSocket.Initializer handler);

  /**
   * Perform a server-sent event handshake and upgrade HTTP GET into a Server-Sent protocol.
   *
   * NOTE: This method is part of Public API, but shouldn't be used by client code.
   *
   * @param handler Server-Sent event handler.
   * @return This context.
   */
  @Nonnull Context upgrade(@Nonnull ServerSentEmitter.Handler handler);

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
  @Nonnull Context setResponseHeader(@Nonnull String name, @Nonnull Date value);

  /**
   * Set response header.
   *
   * @param name Header name.
   * @param value Header value.
   * @return This context.
   */
  @Nonnull Context setResponseHeader(@Nonnull String name, @Nonnull Instant value);

  /**
   * Set response header.
   *
   * @param name Header name.
   * @param value Header value.
   * @return This context.
   */
  @Nonnull Context setResponseHeader(@Nonnull String name, @Nonnull Object value);

  /**
   * Set response header.
   *
   * @param name Header name.
   * @param value Header value.
   * @return This context.
   */
  @Nonnull Context setResponseHeader(@Nonnull String name, @Nonnull String value);

  /**
   * Remove a response header.
   *
   * @param name Header's name.
   * @return This context.
   */
  @Nonnull Context removeResponseHeader(@Nonnull String name);

  /**
   * Clear/reset all the headers, including cookies.
   *
   * @return This context.
   */
  @Nonnull Context removeResponseHeaders();

  /**
   * Set response content length header.
   *
   * @param length Response length.
   * @return This context.
   */
  @Nonnull Context setResponseLength(long length);

  /**
   * Get response header.
   *
   * @param name Header's name.
   * @return Header's value (if set previously) or <code>null</code>.
   */
  @Nullable String getResponseHeader(@Nonnull String name);

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
  @Nonnull Context setResponseCookie(@Nonnull Cookie cookie);

  /**
   * Set response content type header.
   *
   * @param contentType Content type.
   * @return This context.
   */
  @Nonnull Context setResponseType(@Nonnull String contentType);

  /**
   * Set response content type header.
   *
   * @param contentType Content type.
   * @return This context.
   */
  @Nonnull Context setResponseType(@Nonnull MediaType contentType);

  /**
   * Set response content type header.
   *
   * @param contentType Content type.
   * @param charset Charset.
   * @return This context.
   */
  @Nonnull Context setResponseType(@Nonnull MediaType contentType, @Nullable Charset charset);

  /**
   * Set the default response content type header. It is used if the response content type header
   * was not set yet.
   *
   * @param contentType Content type.
   * @return This context.
   */
  @Nonnull Context setDefaultResponseType(@Nonnull MediaType contentType);

  /**
   * Get response content type.
   *
   * @return Response content type.
   */
  @Nonnull MediaType getResponseType();

  /**
   * Set response status code.
   *
   * @param statusCode Status code.
   * @return This context.
   */
  @Nonnull Context setResponseCode(@Nonnull StatusCode statusCode);

  /**
   * Set response status code.
   *
   * @param statusCode Status code.
   * @return This context.
   */
  @Nonnull Context setResponseCode(int statusCode);

  /**
   * Get response status code.
   *
   * @return Response status code.
   */
  @Nonnull StatusCode getResponseCode();

  /**
   * Render a value and send the response to client.
   *
   * @param value Object value.
   * @return This context.
   */
  @Nonnull Context render(@Nonnull Object value);

  /**
   * HTTP response channel as output stream. Usually for chunked responses.
   *
   * @return HTTP channel as output stream. Usually for chunked responses.
   */
  @Nonnull OutputStream responseStream();

  /**
   * HTTP response channel as output stream. Usually for chunked responses.
   *
   * @param contentType Media type.
   * @return HTTP channel as output stream. Usually for chunked responses.
   */
  @Nonnull OutputStream responseStream(@Nonnull MediaType contentType);

  /**
   * HTTP response channel as output stream. Usually for chunked responses.
   *
   * @param contentType Content type.
   * @param consumer Output stream consumer.
   * @return HTTP channel as output stream. Usually for chunked responses.
   * @throws Exception Is something goes wrong.
   */
  @Nonnull Context responseStream(@Nonnull MediaType contentType,
      @Nonnull SneakyThrows.Consumer<OutputStream> consumer) throws Exception;

  /**
   * HTTP response channel as output stream. Usually for chunked responses.
   *
   * @param consumer Output stream consumer.
   * @return HTTP channel as output stream. Usually for chunked responses.
   * @throws Exception Is something goes wrong.
   */
  @Nonnull Context responseStream(@Nonnull SneakyThrows.Consumer<OutputStream> consumer)
      throws Exception;

  /**
   * HTTP response channel as chunker.
   *
   * @return HTTP channel as chunker. Usually for chunked response.
   */
  @Nonnull Sender responseSender();

  /**
   * HTTP response channel as response writer.
   *
   * @return HTTP channel as  response writer. Usually for chunked response.
   */
  @Nonnull PrintWriter responseWriter();

  /**
   * HTTP response channel as response writer.
   *
   * @param contentType Content type.
   * @return HTTP channel as  response writer. Usually for chunked response.
   */
  @Nonnull PrintWriter responseWriter(@Nonnull MediaType contentType);

  /**
   * HTTP response channel as response writer.
   *
   * @param contentType Content type.
   * @param charset Charset.
   * @return HTTP channel as  response writer. Usually for chunked response.
   */
  @Nonnull PrintWriter responseWriter(@Nonnull MediaType contentType, @Nullable Charset charset);

  /**
   * HTTP response channel as response writer.
   *
   * @param consumer Writer consumer.
   * @return This context.
   * @throws Exception Is something goes wrong.
   */
  @Nonnull Context responseWriter(@Nonnull SneakyThrows.Consumer<PrintWriter> consumer)
      throws Exception;

  /**
   * HTTP response channel as response writer.
   *
   * @param contentType Content type.
   * @param consumer Writer consumer.
   * @return This context.
   * @throws Exception Is something goes wrong.
   */
  @Nonnull Context responseWriter(@Nonnull MediaType contentType,
      @Nonnull SneakyThrows.Consumer<PrintWriter> consumer) throws Exception;

  /**
   * HTTP response channel as response writer.
   *
   * @param contentType Content type.
   * @param charset Charset.
   * @param consumer Writer consumer.
   * @return This context.
   * @throws Exception Is something goes wrong.
   */
  @Nonnull Context responseWriter(@Nonnull MediaType contentType, @Nullable Charset charset,
      @Nonnull SneakyThrows.Consumer<PrintWriter> consumer) throws Exception;

  /**
   * Send a <code>302</code> response.
   *
   * @param location Location.
   * @return This context.
   */
  @Nonnull Context sendRedirect(@Nonnull String location);

  /**
   * Send a redirect response.
   *
   * @param redirect Redirect status code.
   * @param location Location.
   * @return This context.
   */
  @Nonnull Context sendRedirect(@Nonnull StatusCode redirect, @Nonnull String location);

  /**
   * Send response data.
   *
   * @param data Response. Use UTF-8 charset.
   * @return This context.
   */
  @Nonnull Context send(@Nonnull String data);

  /**
   * Send response data.
   *
   * @param data Response.
   * @param charset Charset.
   * @return This context.
   */
  @Nonnull Context send(@Nonnull String data, @Nonnull Charset charset);

  /**
   * Send response data.
   *
   * @param data Response.
   * @return This context.
   */
  @Nonnull Context send(@Nonnull byte[] data);

  /**
   * Send response data.
   *
   * @param data Response.
   * @return This context.
   */
  @Nonnull Context send(@Nonnull ByteBuffer data);

  /**
   * Send response data.
   *
   * @param data Response.
   * @return This context.
   */
  @Nonnull Context send(@Nonnull byte[]... data);

  /**
   * Send response data.
   *
   * @param data Response.
   * @return This context.
   */
  @Nonnull Context send(@Nonnull ByteBuffer[] data);

  /**
   * Send response data.
   *
   * @param channel Response input.
   * @return This context.
   */
  @Nonnull Context send(@Nonnull ReadableByteChannel channel);

  /**
   * Send response data.
   *
   * @param input Response.
   * @return This context.
   */
  @Nonnull Context send(@Nonnull InputStream input);

  /**
   * Send a file download response.
   *
   * @param file File download.
   * @return This context.
   */
  @Nonnull Context send(@Nonnull FileDownload file);

  /**
   * Send a file response.
   *
   * @param file File response.
   * @return This context.
   */
  @Nonnull Context send(@Nonnull Path file);

  /**
   * Send a file response.
   *
   * @param file File response.
   * @return This context.
   */
  @Nonnull Context send(@Nonnull FileChannel file);

  /**
   * Send an empty response with the given status code.
   *
   * @param statusCode Status code.
   * @return This context.
   */
  @Nonnull Context send(@Nonnull StatusCode statusCode);

  /**
   * Send an error response. Status code is computed via {@link Router#errorCode(Throwable)}.
   *
   * @param cause Error. If this is a fatal error it is going to be rethrow it.
   * @return This context.
   */
  @Nonnull Context sendError(@Nonnull Throwable cause);

  /**
   * Send an error response.
   *
   * @param cause Error. If this is a fatal error it is going to be rethrow it.
   * @param statusCode Status code.
   * @return This context.
   */
  @Nonnull Context sendError(@Nonnull Throwable cause, @Nonnull StatusCode statusCode);

  /**
   * True if response already started.
   *
   *  @return True if response already started.
   */
  boolean isResponseStarted();

  /**
   * True if response headers are cleared on application error. If none set it uses the
   * default/global value specified by {@link RouterOption#RESET_HEADERS_ON_ERROR}.
   *
   * @return True if response headers are cleared on application error. If none set it uses the
   *     default/global value specified by {@link RouterOption#RESET_HEADERS_ON_ERROR}.
   */
  boolean getResetHeadersOnError();

  /**
   * Set whenever reset/clear headers on application error.
   *
   * @param value True for reset/clear headers.
   * @return This context.
   */
  @Nonnull Context setResetHeadersOnError(boolean value);

  /**
   * Add a complete listener.
   *
   * @param task Task to execute.
   * @return This context.
   */
  @Nonnull Context onComplete(@Nonnull Route.Complete task);

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
  static @Nonnull Context readOnly(@Nonnull Context ctx) {
    return new ReadOnlyContext(ctx);
  }

  /**
   * Wrap a HTTP context and make it WebSocket friendly. Attempt to modify the HTTP response
   * is completely ignored, except for {@link #send(byte[])} and {@link #send(String)} which
   * are delegated to the given web socket.
   *
   * This context is necessary for creating a bridge between {@link MessageEncoder}
   * and {@link WebSocket}.
   *
   * This method is part of Public API, but direct usage is discourage.
   *
   * @param ctx Originating context.
   * @param ws WebSocket.
   * @return Read only context.
   */
  static @Nonnull Context websocket(@Nonnull Context ctx, @Nonnull WebSocket ws) {
    return new WebSocketSender(ctx, ws);
  }
}
