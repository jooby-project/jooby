/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.internal.HashValue;
import io.jooby.internal.MissingValue;
import io.jooby.internal.SingleValue;
import io.jooby.internal.UrlParser;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * HTTP context allows you to interact with the HTTP Request and manipulate the HTTP Response.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface Context {

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
  @Nonnull default <T> T attribute(@Nonnull String key) {
    T attribute = (T) getAttributes().get(key);
    if (attribute == null) {
      attribute = getRouter().attribute(key);
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
  @Nonnull default Context attribute(@Nonnull String key, Object value) {
    getAttributes().put(key, value);
    return this;
  }

  /**
   * Get the HTTP router (usually this represent an instance of {@link Jooby}.
   *
   * @return HTTP router (usually this represent an instance of {@link Jooby}.
   */
  @Nonnull Router getRouter();

  /*
   * **********************************************************************************************
   * **** Request methods *************************************************************************
   * **********************************************************************************************
   */

  /**
   * Find a session or creates a new session.
   *
   * @return Session.
   */
  default @Nonnull Session session() {
    Session session = sessionOrNull();
    if (session == null) {
      Router router = getRouter();
      SessionOptions options = router.getSessionOptions();

      String sessionId = options.generateId();
      session = Session.create(this, options.getStore().newSession(sessionId));
      options.getSessionId().saveSessionId(this, sessionId);
    }
    return session;
  }

  /**
   * Find an existing session.
   *
   * @return Existing session or <code>null</code>.
   */
  default @Nullable Session sessionOrNull() {
    Session session = (Session) getAttributes().get("session");
    if (session == null) {
      Router router = getRouter();
      SessionOptions options = router.getSessionOptions();
      SessionId strategy = options.getSessionId();
      String sessionId = strategy.findSessionId(this);
      if (sessionId == null) {
        return null;
      }
      session = options.getStore().findSession(sessionId);
      if (session == null) {
        return null;
      }
      session = Session.create(this, session);
      strategy.saveSessionId(this, sessionId);
    }
    return session;
  }

  /**
   * Get a cookie matching the given name.
   *
   * @param name Cookie's name.
   * @return Cookie value.
   */
  default @Nonnull Value cookie(@Nonnull String name) {
    String value = cookieMap().get(name);
    return value == null ? Value.missing(name) : Value.value(name, value);
  }

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
   * Matching route.
   *
   * @return Matching route.
   */
  @Nonnull Route getRoute();

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
  @Nonnull String pathString();

  /**
   * Path variable. Value is decoded.
   *
   * @param name Path key.
   * @return Associated value or a missing value, but never a <code>null</code> reference.
   */
  @Nonnull default Value path(@Nonnull String name) {
    String value = pathMap().get(name);
    return value == null
        ? new MissingValue(name)
        : new SingleValue(name, UrlParser.decodePath(value));
  }

  /**
   * Convert the {@link #pathMap()} to the given type.
   *
   * @param type Reified type.
   * @param <T> Target type.
   * @return Instance of target type.
   */
  @Nonnull default <T> T path(@Nonnull Reified<T> type) {
    return path().to(type);
  }

  /**
   * Convert the {@link #pathMap()} to the given type.
   *
   * @param type Target type.
   * @param <T> Target type.
   * @return Instance of target type.
   */
  @Nonnull default <T> T path(@Nonnull Class<T> type) {
    return path().to(type);
  }

  /**
   * Convert {@link #pathMap()} to a {@link Value} object.
   * @return A value object.
   */
  @Nonnull default Value path() {
    HashValue path = new HashValue(null);
    for (Map.Entry<String, String> entry : pathMap().entrySet()) {
      path.put(entry.getKey(), entry.getValue());
    }
    return path;
  }

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
   * Query string as {@link Value} object.
   *
   * @return Query string as {@link Value} object.
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
  @Nonnull default Value query(@Nonnull String name) {
    return query().get(name);
  }

  /**
   * Query string with the leading <code>?</code> or empty string. This is the raw query string,
   * without decoding it.
   *
   * @return Query string with the leading <code>?</code> or empty string. This is the raw query
   *    string, without decoding it.
   */
  @Nonnull default String queryString() {
    return query().queryString();
  }

  /**
   * Convert the queryString to the given type.
   *
   * @param type Reified type.
   * @param <T> Target type.
   * @return Query string converted to target type.
   */
  @Nonnull default <T> T query(@Nonnull Reified<T> type) {
    return query().to(type);
  }

  /**
   * Convert the queryString to the given type.
   *
   * @param type Target type.
   * @param <T> Target type.
   * @return Query string converted to target type.
   */
  @Nonnull default <T> T query(@Nonnull Class<T> type) {
    return query().to(type);
  }

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
  @Nonnull default Map<String, String> queryMap() {
    return query().toMap();
  }

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
  @Nonnull default Map<String, List<String>> queryMultimap() {
    return query().toMultimap();
  }

  /* **********************************************************************************************
   * Header API
   * **********************************************************************************************
   */

  /**
   * Request headers as {@link Value}.
   *
   * @return Request headers as {@link Value}.
   */
  @Nonnull Value headers();

  /**
   * Get a header that matches the given name.
   *
   * @param name Header name. Case insensitive.
   * @return A header value or missing value, never a <code>null</code> reference.
   */
  @Nonnull default Value header(@Nonnull String name) {
    return headers().get(name);
  }

  /**
   * Header as single-value map.
   *
   * @return Header as single-value map.
   */
  @Nonnull default Map<String, String> headerMap() {
    return headers().toMap();
  }

  /**
   * Header as multi-value map.
   *
   * @return Header as multi-value map.
   */
  @Nonnull default Map<String, List<String>> headerMultimap() {
    return headers().toMultimap();
  }

  /**
   * True if the given type matches the `Accept` header. This method returns <code>true</code>
   * if there is no accept header.
   *
   * @param contentType Content type to match.
   * @return True for matching type or if content header is absent.
   */
  default boolean accept(@Nonnull MediaType contentType) {
    Value accept = header(ACCEPT);
    return accept.isMissing() ? true : contentType.matches(accept.value());
  }

  /**
   * Check if the accept type list matches the given produces list and return the most
   * specific media type from produces list.
   *
   * @param produceTypes Produced types.
   * @return The most specific produces type.
   */
  default MediaType accept(@Nonnull List<MediaType> produceTypes) {
    List<MediaType> acceptTypes = MediaType.parse(header(ACCEPT).valueOrNull());
    MediaType result = null;
    for (MediaType acceptType : acceptTypes) {
      for (MediaType produceType : produceTypes) {
        if (produceType.matches(acceptType)) {
          if (result == null) {
            result = produceType;
          } else {
            MediaType max = MediaType.MOST_SPECIFIC.apply(result, produceType);
            if (max != result) {
              result = max;
            }
          }
        }
      }
    }
    return result;
  }

  /**
   * Request <code>Content-Type</code> header or <code>null</code> when missing.
   *
   * @return Request <code>Content-Type</code> header or <code>null</code> when missing.
   */
  @Nullable default MediaType getRequestType() {
    Value contentType = header("Content-Type");
    return contentType.isMissing() ? null : MediaType.valueOf(contentType.value());
  }

  /**
   * Request <code>Content-Type</code> header or <code>null</code> when missing.
   *
   * @param defaults Default content type to use when the header is missing.
   * @return Request <code>Content-Type</code> header or <code>null</code> when missing.
   */
  @Nonnull default MediaType getRequestType(MediaType defaults) {
    Value contentType = header("Content-Type");
    return contentType.isMissing() ? defaults : MediaType.valueOf(contentType.value());
  }

  /**
   * Request <code>Content-Length</code> header or <code>-1</code> when missing.
   *
   * @return Request <code>Content-Length</code> header or <code>-1</code> when missing.
   */
  default long getRequestLength() {
    Value contentLength = header("Content-Length");
    return contentLength.isMissing() ? -1 : contentLength.longValue();
  }

  /**
   * The IP address of the client or last proxy that sent the request.
   *
   * @return The IP address of the client or last proxy that sent the request.
   */
  @Nonnull String getRemoteAddress();

  /**
   * The fully qualified name of the resource being requested, as obtained from the Host HTTP
   * header.
   *
   * @return The fully qualified name of the server.
   */
  default @Nonnull String getHost() {
    return header("host").toOptional()
        .map(host -> {
          int index = host.indexOf(':');
          return index > 0 ? host.substring(0, index) : host;
        })
        .orElse(getRemoteAddress());
  }

  /**
   * The name of the protocol the request. Always in lower-case.
   *
   * @return The name of the protocol the request. Always in lower-case.
   */
  @Nonnull String getProtocol();

  /**
   * HTTP scheme in lower case.
   *
   * @return HTTP scheme in lower case.
   */
  @Nonnull String getScheme();

  /* **********************************************************************************************
   * Form API
   * **********************************************************************************************
   */

  /**
   * Formdata as {@link Value}. This method is for <code>application/form-url-encoded</code>
   * request.
   *
   * @return Formdata as {@link Value}. This method is for <code>application/form-url-encoded</code>
   *    request.
   */
  @Nonnull Formdata form();

  /**
   * Formdata as multi-value map. Only for <code>application/form-url-encoded</code> request.
   *
   * @return Formdata as multi-value map. Only for <code>application/form-url-encoded</code>
   *     request.
   */
  @Nonnull default Map<String, List<String>> formMultimap() {
    return form().toMultimap();
  }

  /**
   * Formdata as single-value map. Only for <code>application/form-url-encoded</code> request.
   *
   * @return Formdata as single-value map. Only for <code>application/form-url-encoded</code>
   *     request.
   */
  @Nonnull default Map<String, String> formMap() {
    return form().toMap();
  }

  /**
   * Form field that matches the given name. Only for <code>application/form-url-encoded</code>
   * request.
   *
   * @param name Field name.
   * @return Form value.
   */
  @Nonnull default Value form(@Nonnull String name) {
    return form().get(name);
  }

  /**
   * Convert formdata to the given type. Only for <code>application/form-url-encoded</code>
   * request.
   *
   * @param type Reified type.
   * @param <T> Target type.
   * @return Formdata as requested type.
   */
  @Nonnull default <T> T form(@Nonnull Reified<T> type) {
    return form().to(type);
  }

  /**
   * Convert formdata to the given type. Only for <code>application/form-url-encoded</code>
   * request.
   *
   * @param type Target type.
   * @param <T> Target type.
   * @return Formdata as requested type.
   */
  @Nonnull default <T> T form(@Nonnull Class<T> type) {
    return form().to(type);
  }

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
   * File upload retrieval is available using {@link Value#fileUpload()} or consider using the
   * {@link #file(String)} instead.
   *
   * Only for <code>multipart/form-data</code> request.
   *
   * @param name Field name.
   * @return Multipart value.
   */
  @Nonnull default Value multipart(@Nonnull String name) {
    return multipart().get(name);
  }

  /**
   * Convert multipart data to the given type.
   *
   * Only for <code>multipart/form-data</code> request.
   *
   * @param type Reified type.
   * @param <T> Target type.
   * @return Target value.
   */
  @Nonnull default <T> T multipart(@Nonnull Reified<T> type) {
    return multipart().to(type);
  }

  /**
   * Convert multipart data to the given type.
   *
   * Only for <code>multipart/form-data</code> request.
   *
   * @param type Target type.
   * @param <T> Target type.
   * @return Target value.
   */
  @Nonnull default <T> T multipart(@Nonnull Class<T> type) {
    return multipart().to(type);
  }

  /**
   * Multipart data as multi-value map.
   *
   * Only for <code>multipart/form-data</code> request.
   *
   * @return Multi-value map.
   */
  @Nonnull default Map<String, List<String>> multipartMultimap() {
    return multipart().toMultimap();
  }

  /**
   * Multipart data as single-value map.
   *
   * Only for <code>multipart/form-data</code> request.
   *
   * @return Single-value map.
   */
  @Nonnull default Map<String, String> multipartMap() {
    return multipart().toMap();
  }

  /**
   * All file uploads. Only for <code>multipart/form-data</code> request.
   *
   * @return All file uploads.
   */
  @Nonnull default List<FileUpload> files() {
    Value multipart = multipart();
    List<FileUpload> result = new ArrayList<>();
    for (Value value : multipart) {
      if (value.isUpload()) {
        result.add(value.fileUpload());
      }
    }
    return result;
  }

  /**
   * All file uploads that matches the given field name.
   *
   * Only for <code>multipart/form-data</code> request.
   *
   * @param name Field name. Please note this is the form field name, not the actual file name.
   * @return All file uploads.
   */
  @Nonnull default List<FileUpload> files(@Nonnull String name) {
    Value multipart = multipart(name);
    List<FileUpload> result = new ArrayList<>();
    for (Value value : multipart) {
      result.add(value.fileUpload());
    }
    return result;
  }

  /**
   * A file upload that matches the given field name.
   *
   * Only for <code>multipart/form-data</code> request.
   *
   * @param name Field name. Please note this is the form field name, not the actual file name.
   * @return A file upload.
   */
  @Nonnull default FileUpload file(@Nonnull String name) {
    return multipart(name).fileUpload();
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
  default @Nonnull <T> T body(@Nonnull Reified<T> type) {
    return body(type, getRequestType(MediaType.text));
  }

  /**
   * Convert the HTTP body to the given type.
   *
   * @param type Reified type.
   * @param contentType Body content type.
   * @param <T> Conversion type.
   * @return Instance of conversion type.
   */
  default @Nonnull <T> T body(@Nonnull Reified<T> type, @Nonnull MediaType contentType) {
    try {
      return parser(contentType).parse(this, type.getType());
    } catch (Exception x) {
      throw Sneaky.propagate(x);
    }
  }

  /**
   * Convert the HTTP body to the given type.
   *
   * @param type Reified type.
   * @param <T> Conversion type.
   * @return Instance of conversion type.
   */
  default @Nonnull <T> T body(@Nonnull Class type) {
    return body(type, getRequestType(MediaType.text));
  }

  /**
   * Convert the HTTP body to the given type.
   *
   * @param type Reified type.
   * @param contentType Body content type.
   * @param <T> Conversion type.
   * @return Instance of conversion type.
   */
  default @Nonnull <T> T body(@Nonnull Class type, @Nonnull MediaType contentType) {
    try {
      return parser(contentType).parse(this, type);
    } catch (Exception x) {
      throw Sneaky.propagate(x);
    }
  }

  /* **********************************************************************************************
   * Body Parser
   * **********************************************************************************************
   */

  /**
   * Get a parser for the given content type or get an {@link StatusCode#UNSUPPORTED_MEDIA_TYPE}.
   *
   * @param contentType Content type.
   * @return Parser.
   */
  default @Nonnull Parser parser(@Nonnull MediaType contentType) {
    return getRoute().parser(contentType);
  }

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
   * This operations integrates easily with third-party libraries liks rxJava or others.
   *
   * @param action Application code.
   * @return This context.
   */
  @Nonnull Context detach(@Nonnull Runnable action);

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
  @Nonnull default Context setResponseHeader(@Nonnull String name, @Nonnull Date value) {
    return setResponseHeader(name, RFC1123.format(Instant.ofEpochMilli(value.getTime())));
  }

  /**
   * Set response header.
   *
   * @param name Header name.
   * @param value Header value.
   * @return This context.
   */
  @Nonnull default Context setResponseHeader(@Nonnull String name, @Nonnull Instant value) {
    return setResponseHeader(name, RFC1123.format(value));
  }

  /**
   * Set response header.
   *
   * @param name Header name.
   * @param value Header value.
   * @return This context.
   */
  @Nonnull default Context setResponseHeader(@Nonnull String name, @Nonnull Object value) {
    if (value instanceof Date) {
      return setResponseHeader(name, (Date) value);
    }
    if (value instanceof Instant) {
      return setResponseHeader(name, (Instant) value);
    }
    return setResponseHeader(name, value.toString());
  }

  /**
   * Set response header.
   *
   * @param name Header name.
   * @param value Header value.
   * @return This context.
   */
  @Nonnull Context setResponseHeader(@Nonnull String name, @Nonnull String value);

  @Nonnull Context removeResponseHeader(@Nonnull String name);

  /**
   * Set response content length header.
   *
   * @param length Response length.
   * @return This context.
   */
  @Nonnull Context setResponseLength(long length);

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
  @Nonnull default Context setResponseType(@Nonnull MediaType contentType) {
    return setResponseType(contentType, contentType.getCharset());
  }

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
  @Nonnull default Context setResponseCode(@Nonnull StatusCode statusCode) {
    return setResponseCode(statusCode.value());
  }

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
  default @Nonnull Context render(@Nonnull Object value) {
    try {
      Route route = getRoute();
      Renderer renderer = route.getRenderer();
      byte[] bytes = renderer.render(this, value);
      send(bytes);
      return this;
    } catch (Exception x) {
      throw Sneaky.propagate(x);
    }
  }

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
  default @Nonnull OutputStream responseStream(@Nonnull MediaType contentType) {
    setResponseType(contentType);
    return responseStream();
  }

  /**
   * HTTP response channel as output stream. Usually for chunked responses.
   *
   * @param contentType Content type.
   * @param consumer Output stream consumer.
   * @return HTTP channel as output stream. Usually for chunked responses.
   * @throws Exception Is something goes wrong.
   */
  default @Nonnull Context responseStream(@Nonnull MediaType contentType,
      @Nonnull Sneaky.Consumer<OutputStream> consumer) throws Exception {
    setResponseType(contentType);
    return responseStream(consumer);
  }

  /**
   * HTTP response channel as output stream. Usually for chunked responses.
   *
   * @param consumer Output stream consumer.
   * @return HTTP channel as output stream. Usually for chunked responses.
   * @throws Exception Is something goes wrong.
   */
  default @Nonnull Context responseStream(@Nonnull Sneaky.Consumer<OutputStream> consumer)
      throws Exception {
    try (OutputStream out = responseStream()) {
      consumer.accept(out);
    }
    return this;
  }

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
  default @Nonnull PrintWriter responseWriter() {
    return responseWriter(MediaType.text);
  }

  /**
   * HTTP response channel as response writer.
   *
   * @param contentType Content type.
   * @return HTTP channel as  response writer. Usually for chunked response.
   */
  default @Nonnull PrintWriter responseWriter(@Nonnull MediaType contentType) {
    return responseWriter(contentType, contentType.getCharset());
  }

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
  default @Nonnull Context responseWriter(@Nonnull Sneaky.Consumer<PrintWriter> consumer)
      throws Exception {
    return responseWriter(MediaType.text, consumer);
  }

  /**
   * HTTP response channel as response writer.
   *
   * @param contentType Content type.
   * @param consumer Writer consumer.
   * @return This context.
   * @throws Exception Is something goes wrong.
   */
  default @Nonnull Context responseWriter(@Nonnull MediaType contentType,
      @Nonnull Sneaky.Consumer<PrintWriter> consumer) throws Exception {
    return responseWriter(contentType, contentType.getCharset(), consumer);
  }

  /**
   * HTTP response channel as response writer.
   *
   * @param contentType Content type.
   * @param charset Charset.
   * @param consumer Writer consumer.
   * @return This context.
   * @throws Exception Is something goes wrong.
   */
  default @Nonnull Context responseWriter(@Nonnull MediaType contentType, @Nullable Charset charset,
      @Nonnull Sneaky.Consumer<PrintWriter> consumer) throws Exception {
    try (PrintWriter writer = responseWriter(contentType, charset)) {
      consumer.accept(writer);
    }
    return this;
  }

  /**
   * Send a <code>302</code> response.
   *
   * @param location Location.
   * @return This context.
   */
  default @Nonnull Context sendRedirect(@Nonnull String location) {
    return sendRedirect(StatusCode.FOUND, location);
  }

  /**
   * Send a redirect response.
   *
   * @param redirect Redirect status code.
   * @param location Location.
   * @return This context.
   */
  default @Nonnull Context sendRedirect(@Nonnull StatusCode redirect, @Nonnull String location) {
    setResponseHeader("location", location);
    return send(redirect);
  }

  /**
   * Send response data.
   *
   * @param data Response. Use UTF-8 charset.
   * @return This context.
   */
  default @Nonnull Context send(@Nonnull String data) {
    return send(data, StandardCharsets.UTF_8);
  }

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
  default @Nonnull Context send(@Nonnull ByteBuf data) {
    return send(data.nioBuffer());
  }

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
   * Send a file attached response.
   *
   * @param file Attached file.
   * @return This context.
   */
  default @Nonnull Context send(@Nonnull AttachedFile file) {
    setResponseHeader("Content-Disposition", file.getContentDisposition());
    InputStream content = file.stream();
    long length = file.getFileSize();
    if (length > 0) {
      setResponseLength(length);
    }
    setDefaultResponseType(file.getContentType());
    if (content instanceof FileInputStream) {
      send(((FileInputStream) content).getChannel());
    } else {
      send(content);
    }
    return this;
  }

  /**
   * Send a file response.
   *
   * @param file File response.
   * @return This context.
   */
  default @Nonnull Context send(@Nonnull Path file) {
    try {
      setDefaultResponseType(MediaType.byFile(file));
      return send(FileChannel.open(file));
    } catch (IOException x) {
      throw Sneaky.propagate(x);
    }
  }

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
  @Nonnull default Context sendError(@Nonnull Throwable cause) {
    sendError(cause, getRouter().errorCode(cause));
    return this;
  }

  /**
   * Send an error response.
   *
   * @param cause Error. If this is a fatal error it is going to be rethrow it.
   * @param statusCode Status code.
   * @return This context.
   */
  @Nonnull default Context sendError(@Nonnull Throwable cause, @Nonnull StatusCode statusCode) {
    Router router = getRouter();
    try {
      router.getErrorHandler().apply(this, cause, statusCode);
    } catch (Exception x) {
      router.getLog()
          .error("error handler resulted in exception {} {}", getMethod(), pathString(), x);
    }
    /** rethrow fatal exceptions: */
    if (Sneaky.isFatal(cause)) {
      throw Sneaky.propagate(cause);
    }
    return this;
  }

  /**
   * True if response already started.
   *
   *  @return True if response already started.
   */
  boolean isResponseStarted();

}
