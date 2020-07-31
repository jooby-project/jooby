/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.exception.RegistryException;
import io.jooby.exception.TypeMismatchException;
import io.jooby.internal.HashValue;
import io.jooby.internal.MissingValue;
import io.jooby.internal.SingleValue;
import io.jooby.internal.UrlParser;
import io.jooby.internal.ValueConverters;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/***
 * Like {@link Context} but with couple of default methods.
 *
 * @since 2.0.2
 * @author edgar
 */
public interface DefaultContext extends Context {

  @Nonnull @Override default <T> T require(@Nonnull Class<T> type, @Nonnull String name)
      throws RegistryException {
    return getRouter().require(type, name);
  }

  @Nonnull @Override default <T> T require(@Nonnull Class<T> type) throws RegistryException {
    return getRouter().require(type);
  }

  @Nonnull @Override default <T> T require(@Nonnull ServiceKey<T> key) throws RegistryException {
    return getRouter().require(key);
  }

  @Nullable @Override default <T> T getUser() {
    return (T) getAttributes().get("user");
  }

  @Nonnull @Override default Context setUser(@Nullable Object user) {
    getAttributes().put("user", user);
    return this;
  }

  @Override default boolean matches(String pattern) {
    return getRouter().match(pattern, getRequestPath());
  }

  /**
   * Get an attribute by his key. This is just an utility method around {@link #getAttributes()}.
   * This method look first in current context and fallback to application attributes.
   *
   * @param key Attribute key.
   * @param <T> Attribute type.
   * @return Attribute value.
   */
  @Override @Nonnull default <T> T attribute(@Nonnull String key) {
    T attribute = (T) getAttributes().get(key);
    if (attribute == null) {
      Map<String, Object> globals = getRouter().getAttributes();
      attribute = (T) globals.get(key);
    }
    return attribute;
  }

  @Override @Nonnull default Context attribute(@Nonnull String key, Object value) {
    getAttributes().put(key, value);
    return this;
  }

  @Override default @Nonnull FlashMap flash() {
    return (FlashMap) getAttributes()
        .computeIfAbsent(FlashMap.NAME, key -> FlashMap
            .create(this, new Cookie(getRouter().getFlashCookie()).setHttpOnly(true)));
  }

  /**
   * Get a flash attribute.
   *
   * @param name Attribute's name.
   * @return Flash attribute.
   */
  @Override default @Nonnull Value flash(@Nonnull String name) {
    return Value.create(this, name, flash().get(name));
  }

  @Override default @Nonnull Value session(@Nonnull String name) {
    Session session = sessionOrNull();
    if (session != null) {
      return session.get(name);
    }
    return Value.missing(name);
  }

  @Override default @Nonnull Session session() {
    Session session = sessionOrNull();
    if (session == null) {
      SessionStore store = getRouter().getSessionStore();
      session = store.newSession(this);
      getAttributes().put(Session.NAME, session);
    }
    return session;
  }

  @Override default @Nullable Session sessionOrNull() {
    Session session = (Session) getAttributes().get(Session.NAME);
    if (session == null) {
      Router router = getRouter();
      SessionStore store = router.getSessionStore();
      session = store.findSession(this);
      if (session != null) {
        getAttributes().put(Session.NAME, session);
      }
    }
    return session;
  }

  @Override default @Nonnull Context forward(@Nonnull String path) {
    setRequestPath(path);
    getRouter().match(this).execute(this);
    return this;
  }

  @Override default @Nonnull Value cookie(@Nonnull String name) {
    String value = cookieMap().get(name);
    return value == null ? Value.missing(name) : Value.value(this, name, value);
  }

  @Override @Nonnull default Value path(@Nonnull String name) {
    String value = pathMap().get(name);
    return value == null
        ? new MissingValue(name)
        : new SingleValue(this, name, UrlParser.decodePathSegment(value));
  }

  @Override @Nonnull default <T> T path(@Nonnull Class<T> type) {
    return path().to(type);
  }

  @Override @Nonnull default ValueNode path() {
    HashValue path = new HashValue(this, null);
    for (Map.Entry<String, String> entry : pathMap().entrySet()) {
      path.put(entry.getKey(), entry.getValue());
    }
    return path;
  }

  @Override @Nonnull default ValueNode query(@Nonnull String name) {
    return query().get(name);
  }

  @Override @Nonnull default String queryString() {
    return query().queryString();
  }

  @Override @Nonnull default <T> T query(@Nonnull Class<T> type) {
    return query().to(type);
  }

  @Override @Nonnull default Map<String, String> queryMap() {
    return query().toMap();
  }

  @Override @Nonnull default Map<String, List<String>> queryMultimap() {
    return query().toMultimap();
  }

  @Override @Nonnull default Value header(@Nonnull String name) {
    return header().get(name);
  }

  @Override @Nonnull default Map<String, String> headerMap() {
    return header().toMap();
  }

  @Override @Nonnull default Map<String, List<String>> headerMultimap() {
    return header().toMultimap();
  }

  @Override default boolean accept(@Nonnull MediaType contentType) {
    Value accept = header(ACCEPT);
    return accept.isMissing() || contentType.matches(accept.value());
  }

  @Override default MediaType accept(@Nonnull List<MediaType> produceTypes) {
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

  @Override default @Nonnull String getRequestURL() {
    return getRequestURL("");
  }

  @Override default @Nonnull String getRequestURL(@Nonnull String path) {
    String scheme = getScheme();
    String host = getHost();
    int port = getPort();
    StringBuilder url = new StringBuilder();
    url.append(scheme).append("://").append(host);
    if (port > 0 && port != PORT && port != SECURE_PORT) {
      url.append(":").append(port);
    }
    if (path == null || path.length() == 0) {
      url.append(getRequestPath());
    } else {
      String contextPath = getContextPath();
      if (!contextPath.equals("/") && !path.startsWith(contextPath)) {
        url.append(contextPath);
      }
      url.append(path);
    }
    url.append(queryString());

    return url.toString();
  }

  @Override @Nullable default MediaType getRequestType() {
    Value contentType = header("Content-Type");
    return contentType.isMissing() ? null : MediaType.valueOf(contentType.value());
  }

  @Override @Nonnull default MediaType getRequestType(MediaType defaults) {
    Value contentType = header("Content-Type");
    return contentType.isMissing() ? defaults : MediaType.valueOf(contentType.value());
  }

  @Override default long getRequestLength() {
    Value contentLength = header("Content-Length");
    return contentLength.isMissing() ? -1 : contentLength.longValue();
  }

  @Override default @Nullable String getHostAndPort() {
    Optional<String> header = getRouter().isTrustProxy()
        ? header("X-Forwarded-Host").toOptional()
        : Optional.empty();
    String value = header
        .orElseGet(() -> header("Host").value(getServerHost() + ":" + getServerPort()));
    int i = value.indexOf(',');
    String host = i > 0 ? value.substring(0, i).trim() : value;
    if (host.startsWith("[") && host.endsWith("]")) {
      return host.substring(1, host.length() - 1).trim();
    }
    return host;
  }

  @Override default @Nonnull String getServerHost() {
    String host = getRouter().getServerOptions().getHost();
    return host.equals("0.0.0.0") ? "localhost" : host;
  }

  @Override default int getServerPort() {
    ServerOptions options = getRouter().getServerOptions();
    return isSecure() ? options.getSecurePort() : options.getPort();
  }

  @Override default int getPort() {
    String hostAndPort = getHostAndPort();
    if (hostAndPort != null) {
      int index = hostAndPort.indexOf(':');
      if (index > 0) {
        return Integer.parseInt(hostAndPort.substring(index + 1));
      }
      return isSecure() ? SECURE_PORT : PORT;
    }
    return getServerPort();
  }

  @Override default @Nonnull String getHost() {
    String hostAndPort = getHostAndPort();
    if (hostAndPort != null) {
      int index = hostAndPort.indexOf(':');
      return index > 0 ? hostAndPort.substring(0, index).trim() : hostAndPort;
    }
    return getServerHost();
  }

  @Override default boolean isSecure() {
    return getScheme().equals("https");
  }

  @Override @Nonnull default Map<String, List<String>> formMultimap() {
    return form().toMultimap();
  }

  @Override @Nonnull default Map<String, String> formMap() {
    return form().toMap();
  }

  @Override @Nonnull default ValueNode form(@Nonnull String name) {
    return form().get(name);
  }

  @Override @Nonnull default <T> T form(@Nonnull Class<T> type) {
    return form().to(type);
  }

  @Override @Nonnull default ValueNode multipart(@Nonnull String name) {
    return multipart().get(name);
  }

  @Override @Nonnull default <T> T multipart(@Nonnull Class<T> type) {
    return multipart().to(type);
  }

  @Override @Nonnull default Map<String, List<String>> multipartMultimap() {
    return multipart().toMultimap();
  }

  @Override @Nonnull default Map<String, String> multipartMap() {
    return multipart().toMap();
  }

  @Override @Nonnull default List<FileUpload> files() {
    return multipart().files();
  }

  @Override @Nonnull default List<FileUpload> files(@Nonnull String name) {
    return multipart().files(name);
  }

  @Override @Nonnull default FileUpload file(@Nonnull String name) {
    return multipart().file(name);
  }

  @Override default @Nonnull <T> T body(@Nonnull Class<T> type) {
    return body().to(type);
  }

  @Override default @Nonnull <T> T body(@Nonnull Type type) {
    return body().to(type);
  }

  @Override default @Nullable <T> T convert(ValueNode value, Class<T> type) {
    T result = ValueConverters.convert(value, type, getRouter());
    if (result == null) {
      throw new TypeMismatchException(value.name(), type);
    }
    return result;
  }

  @Override default @Nonnull <T> T decode(@Nonnull Type type, @Nonnull MediaType contentType) {
    try {
      if (MediaType.text.equals(contentType)) {
        T result = ValueConverters.convert(body(), type, getRouter());
        return result;
      }
      return (T) decoder(contentType).decode(this, type);
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override default @Nonnull MessageDecoder decoder(@Nonnull MediaType contentType) {
    return getRoute().decoder(contentType);
  }

  @Override @Nonnull default Context setResponseHeader(@Nonnull String name, @Nonnull Date value) {
    return setResponseHeader(name, RFC1123.format(Instant.ofEpochMilli(value.getTime())));
  }

  @Override @Nonnull
  default Context setResponseHeader(@Nonnull String name, @Nonnull Instant value) {
    return setResponseHeader(name, RFC1123.format(value));
  }

  @Override @Nonnull
  default Context setResponseHeader(@Nonnull String name, @Nonnull Object value) {
    if (value instanceof Date) {
      return setResponseHeader(name, (Date) value);
    }
    if (value instanceof Instant) {
      return setResponseHeader(name, (Instant) value);
    }
    return setResponseHeader(name, value.toString());
  }

  @Override @Nonnull default Context setResponseType(@Nonnull MediaType contentType) {
    return setResponseType(contentType, contentType.getCharset());
  }

  @Override @Nonnull default Context setResponseCode(@Nonnull StatusCode statusCode) {
    return setResponseCode(statusCode.value());
  }

  @Override default @Nonnull Context render(@Nonnull Object value) {
    try {
      Route route = getRoute();
      MessageEncoder encoder = route.getEncoder();
      byte[] bytes = encoder.encode(this, value);
      if (bytes == null) {
        if (!isResponseStarted()) {
          throw new IllegalStateException("The response was not encoded");
        }
      } else {
        send(bytes);
      }
      return this;
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override default @Nonnull OutputStream responseStream(@Nonnull MediaType contentType) {
    setResponseType(contentType);
    return responseStream();
  }

  @Override default @Nonnull Context responseStream(@Nonnull MediaType contentType,
      @Nonnull SneakyThrows.Consumer<OutputStream> consumer) throws Exception {
    setResponseType(contentType);
    return responseStream(consumer);
  }

  @Override default @Nonnull Context responseStream(
      @Nonnull SneakyThrows.Consumer<OutputStream> consumer)
      throws Exception {
    try (OutputStream out = responseStream()) {
      consumer.accept(out);
    }
    return this;
  }

  @Override default @Nonnull PrintWriter responseWriter() {
    return responseWriter(MediaType.text);
  }

  @Override default @Nonnull PrintWriter responseWriter(@Nonnull MediaType contentType) {
    return responseWriter(contentType, contentType.getCharset());
  }

  @Override default @Nonnull Context responseWriter(
      @Nonnull SneakyThrows.Consumer<PrintWriter> consumer)
      throws Exception {
    return responseWriter(MediaType.text, consumer);
  }

  @Override default @Nonnull Context responseWriter(@Nonnull MediaType contentType,
      @Nonnull SneakyThrows.Consumer<PrintWriter> consumer) throws Exception {
    return responseWriter(contentType, contentType.getCharset(), consumer);
  }

  @Override default @Nonnull Context responseWriter(@Nonnull MediaType contentType,
      @Nullable Charset charset,
      @Nonnull SneakyThrows.Consumer<PrintWriter> consumer) throws Exception {
    try (PrintWriter writer = responseWriter(contentType, charset)) {
      consumer.accept(writer);
    }
    return this;
  }

  @Override default @Nonnull Context sendRedirect(@Nonnull String location) {
    return sendRedirect(StatusCode.FOUND, location);
  }

  @Override default @Nonnull Context sendRedirect(@Nonnull StatusCode redirect,
      @Nonnull String location) {
    setResponseHeader("location", location);
    return send(redirect);
  }

  @Override default @Nonnull Context send(@Nonnull byte[]... data) {
    ByteBuffer[] buffer = new ByteBuffer[data.length];
    for (int i = 0; i < data.length; i++) {
      buffer[i] = ByteBuffer.wrap(data[i]);
    }
    return send(buffer);
  }

  @Override default @Nonnull Context send(@Nonnull String data) {
    return send(data, StandardCharsets.UTF_8);
  }

  @Override default @Nonnull Context send(@Nonnull AttachedFile file) {
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

  @Override default @Nonnull Context send(@Nonnull Path file) {
    try {
      setDefaultResponseType(MediaType.byFile(file));
      return send(FileChannel.open(file));
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override @Nonnull default Context sendError(@Nonnull Throwable cause) {
    sendError(cause, getRouter().errorCode(cause));
    return this;
  }

  /**
   * Send an error response. This method set the  error code.
   *
   * @param cause Error. If this is a fatal error it is going to be rethrow it.
   * @param code Default error code.
   * @return This context.
   */
  @Override @Nonnull default Context sendError(@Nonnull Throwable cause,
      @Nonnull StatusCode code) {
    Router router = getRouter();
    Logger log = router.getLog();
    if (isResponseStarted()) {
      log.error(ErrorHandler.errorMessage(this, code), cause);
    } else {
      try {
        if (getResetHeadersOnError()) {
          removeResponseHeaders();
        }
        // set default error code
        setResponseCode(code);
        router.getErrorHandler().apply(this, cause, code);
      } catch (Exception x) {
        if (!isResponseStarted()) {
          // edge case when there is a bug in a the error handler (probably custom error) what we
          // do is to use the default error handler
          ErrorHandler.create().apply(this, cause, code);
        }
        if (Server.connectionLost(x)) {
          log.debug("error handler resulted in a exception while processing `{}`", cause.toString(),
              x);
        } else {
          log.error("error handler resulted in a exception while processing `{}`", cause.toString(),
              x);
        }
      }
    }
    /** rethrow fatal exceptions: */
    if (SneakyThrows.isFatal(cause)) {
      throw SneakyThrows.propagate(cause);
    }
    return this;
  }
}
