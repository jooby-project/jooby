/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

import org.slf4j.Logger;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.exception.RegistryException;
import io.jooby.internal.*;
import io.jooby.output.OutputFactory;
import io.jooby.value.Value;
import io.jooby.value.ValueFactory;

/***
 * Like {@link Context} but with couple of default methods.
 *
 * @since 2.0.2
 * @author edgar
 */
public interface DefaultContext extends Context {

  @Override
  default <T> T require(@NonNull Class<T> type, @NonNull String name) throws RegistryException {
    return getRouter().require(type, name);
  }

  @Override
  default <T> T require(@NonNull Class<T> type) throws RegistryException {
    return getRouter().require(type);
  }

  @Override
  default <T> T require(@NonNull Reified<T> type) throws RegistryException {
    return getRouter().require(type);
  }

  @Override
  default <T> T require(@NonNull Reified<T> type, @NonNull String name) throws RegistryException {
    return getRouter().require(type, name);
  }

  @Override
  default <T> T require(@NonNull ServiceKey<T> key) throws RegistryException {
    return getRouter().require(key);
  }

  @SuppressWarnings("unchecked")
  @Nullable @Override
  default <T> T getUser() {
    return (T) getAttributes().get("user");
  }

  @Override
  default Context setUser(@Nullable Object user) {
    getAttributes().put("user", user);
    return this;
  }

  @Override
  default boolean matches(@NonNull String pattern) {
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
  @Override
  @Nullable default <T> T getAttribute(@NonNull String key) {
    T attribute = (T) getAttributes().get(key);
    if (attribute == null) {
      Map<String, Object> globals = getRouter().getAttributes();
      attribute = (T) globals.get(key);
    }
    return attribute;
  }

  @Override
  default Context setAttribute(@NonNull String key, Object value) {
    getAttributes().put(key, value);
    return this;
  }

  @Override
  default FlashMap flash() {
    return (FlashMap)
        getAttributes()
            .computeIfAbsent(
                FlashMap.NAME, key -> FlashMap.create(this, getRouter().getFlashCookie().clone()));
  }

  @Nullable @Override
  default FlashMap flashOrNull() {
    var flashCookie = cookie(getRouter().getFlashCookie().getName());
    return flashCookie.isMissing() ? null : flash();
  }

  /**
   * Get a flash attribute.
   *
   * @param name Attribute's name.
   * @return Flash attribute.
   */
  @Override
  default Value flash(@NonNull String name) {
    return Value.create(getValueFactory(), name, flash().get(name));
  }

  @Override
  default Value session(@NonNull String name) {
    Session session = sessionOrNull();
    if (session != null) {
      return session.get(name);
    }
    return Value.missing(getValueFactory(), name);
  }

  @Override
  default Session session() {
    Session session = sessionOrNull();
    if (session == null) {
      SessionStore store = getRouter().getSessionStore();
      session = store.newSession(this);
      getAttributes().put(Session.NAME, session);
    }
    return session;
  }

  @Override
  default @Nullable Session sessionOrNull() {
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

  @Override
  default Object forward(@NonNull String path) {
    try {
      setRequestPath(path);
      Router.Match match = getRouter().match(this);
      return match.execute(this, match.route().getHandler());
    } catch (Throwable cause) {
      throw SneakyThrows.propagate(cause);
    }
  }

  @Override
  default Value cookie(@NonNull String name) {
    String value = cookieMap().get(name);
    return value == null
        ? Value.missing(getValueFactory(), name)
        : Value.value(getValueFactory(), name, value);
  }

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
  default ParamLookup lookup() {
    return new ParamLookupImpl(this);
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
  default Value lookup(@NonNull String name, ParamSource... sources) {
    if (sources.length == 0) {
      throw new IllegalArgumentException("No parameter sources were specified.");
    }

    return Arrays.stream(sources)
        .map(source -> source.provider.apply(this, name))
        .filter(value -> !value.isMissing())
        .findFirst()
        .orElseGet(() -> Value.missing(getValueFactory(), name));
  }

  @Override
  default Value path(@NonNull String name) {
    String value = pathMap().get(name);
    return value == null
        ? new MissingValue(getValueFactory(), name)
        : new SingleValue(getValueFactory(), name, UrlParser.decodePathSegment(value));
  }

  @Override
  default <T> T path(@NonNull Class<T> type) {
    return path().to(type);
  }

  @Override
  default Value path() {
    var path = new HashValue(getValueFactory(), null);
    for (Map.Entry<String, String> entry : pathMap().entrySet()) {
      path.put(entry.getKey(), entry.getValue());
    }
    return path;
  }

  @Override
  default Value query(@NonNull String name) {
    return query().get(name);
  }

  @Override
  default String queryString() {
    return query().queryString();
  }

  @Override
  default <T> T query(@NonNull Class<T> type) {
    return query().toEmpty(type);
  }

  @Override
  default Map<String, String> queryMap() {
    return query().toMap();
  }

  @Override
  default Value header(@NonNull String name) {
    return header().get(name);
  }

  @Override
  default Map<String, String> headerMap() {
    return header().toMap();
  }

  @Override
  default boolean accept(@NonNull MediaType contentType) {
    return Objects.equals(accept(singletonList(contentType)), contentType);
  }

  @Override
  default @Nullable MediaType accept(@NonNull List<MediaType> produceTypes) {
    var accept = header(ACCEPT);
    if (accept.isMissing()) {
      // NO header? Pick first, which is the default.
      return produceTypes.isEmpty() ? null : produceTypes.get(0);
    }

    // Sort accept by most relevant/specific first:
    var acceptTypes =
        accept.toList().stream()
            .flatMap(value -> MediaType.parse(value).stream())
            .distinct()
            .sorted()
            .toList();

    // Find most appropriated type:
    var idx = Integer.MAX_VALUE;
    MediaType result = null;
    for (var produceType : produceTypes) {
      for (int i = 0; i < acceptTypes.size(); i++) {
        MediaType acceptType = acceptTypes.get(i);
        if (produceType.matches(acceptType)) {
          if (i < idx) {
            result = produceType;
            idx = i;
            break;
          }
        }
      }
    }
    return result;
  }

  @Override
  default String getRequestURL() {
    return getRequestURL(getRequestPath() + queryString());
  }

  @Override
  default String getRequestURL(@NonNull String path) {
    var scheme = getScheme();
    var host = getHost();
    int port = getPort();
    var url = new StringBuilder();
    url.append(scheme).append("://").append(host);
    if (port > 0 && port != PORT && port != SECURE_PORT) {
      url.append(":").append(port);
    }
    var contextPath = getContextPath();
    if (!contextPath.equals("/") && !path.startsWith(contextPath)) {
      url.append(contextPath);
    }
    url.append(path);

    return url.toString();
  }

  @Override
  @Nullable default MediaType getRequestType() {
    Value contentType = header("Content-Type");
    return contentType.isMissing() ? null : MediaType.valueOf(contentType.value());
  }

  @Override
  default MediaType getRequestType(MediaType defaults) {
    Value contentType = header("Content-Type");
    return contentType.isMissing() ? defaults : MediaType.valueOf(contentType.value());
  }

  @Override
  default long getRequestLength() {
    Value contentLength = header("Content-Length");
    return contentLength.isMissing() ? -1 : contentLength.longValue();
  }

  @Override
  default String getHostAndPort() {
    Optional<String> header =
        getRouter().getRouterOptions().isTrustProxy()
            ? header("X-Forwarded-Host").toOptional()
            : Optional.empty();
    var value =
        header.orElseGet(
            () ->
                ofNullable(header("Host").valueOrNull())
                    .orElseGet(() -> getServerHost() + ":" + getServerPort()));
    int i = value.indexOf(',');
    String host = i > 0 ? value.substring(0, i).trim() : value;
    if (host.startsWith("[") && host.endsWith("]")) {
      return host.substring(1, host.length() - 1).trim();
    }
    return host;
  }

  @Override
  default String getServerHost() {
    var host = require(ServerOptions.class).getHost();
    return host.equals("0.0.0.0") ? "localhost" : host;
  }

  @Override
  default int getServerPort() {
    var options = require(ServerOptions.class);
    return isSecure()
        // Buggy proxy where it report a https scheme but there is no HTTPS configured option
        ? ofNullable(options.getSecurePort()).orElse(options.getPort())
        : options.getPort();
  }

  @Override
  default int getPort() {
    var hostAndPort = getHostAndPort();
    if (hostAndPort != null) {
      int index = hostAndPort.indexOf(':');
      if (index > 0) {
        return Integer.parseInt(hostAndPort.substring(index + 1));
      }
      return isSecure() ? SECURE_PORT : PORT;
    }
    return getServerPort();
  }

  @Override
  default String getHost() {
    String hostAndPort = getHostAndPort();
    if (hostAndPort != null) {
      int index = hostAndPort.indexOf(':');
      return index > 0 ? hostAndPort.substring(0, index).trim() : hostAndPort;
    }
    return getServerHost();
  }

  @Override
  default boolean isSecure() {
    return getScheme().equals("https");
  }

  @Override
  default Value form(@NonNull String name) {
    return form().get(name);
  }

  @Override
  default <T> T form(@NonNull Class<T> type) {
    return form().to(type);
  }

  @Override
  default Map<String, String> formMap() {
    return form().toMap();
  }

  @Override
  default List<FileUpload> files() {
    return form().files();
  }

  @Override
  default List<FileUpload> files(@NonNull String name) {
    return form().files(name);
  }

  @Override
  default FileUpload file(@NonNull String name) {
    return form().file(name);
  }

  @Override
  default <T> T body(@NonNull Class<T> type) {
    return body().to(type);
  }

  @Override
  default <T> T body(@NonNull Type type) {
    return body().to(type);
  }

  default ValueFactory getValueFactory() {
    return getRouter().getValueFactory();
  }

  @Override
  default <T> T decode(@NonNull Type type, @NonNull MediaType contentType) {
    try {
      if (MediaType.text.equals(contentType)) {
        return getValueFactory().convert(type, body());
      }
      //noinspection unchecked
      return (T) decoder(contentType).decode(this, type);
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override
  default MessageDecoder decoder(@NonNull MediaType contentType) {
    return getRoute().decoder(contentType);
  }

  @Override
  default Context setResponseHeader(@NonNull String name, @NonNull Date value) {
    return setResponseHeader(name, RFC1123.format(Instant.ofEpochMilli(value.getTime())));
  }

  @Override
  default Context setResponseHeader(@NonNull String name, @NonNull Instant value) {
    return setResponseHeader(name, RFC1123.format(value));
  }

  @Override
  default Context setResponseHeader(@NonNull String name, @NonNull Object value) {
    if (value instanceof Date) {
      return setResponseHeader(name, (Date) value);
    }
    if (value instanceof Instant) {
      return setResponseHeader(name, (Instant) value);
    }
    return setResponseHeader(name, value.toString());
  }

  @Override
  default Context setResponseCode(@NonNull StatusCode statusCode) {
    return setResponseCode(statusCode.value());
  }

  @Override
  default Context render(@NonNull Object value) {
    try {
      var route = getRoute();
      var encoder = route.getEncoder();
      var bytes = encoder.encode(this, value);
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

  @Override
  default OutputStream responseStream(@NonNull MediaType contentType) {
    setResponseType(contentType);
    return responseStream();
  }

  @Override
  default Context responseStream(
      @NonNull MediaType contentType, @NonNull SneakyThrows.Consumer<OutputStream> consumer)
      throws Exception {
    setResponseType(contentType);
    return responseStream(consumer);
  }

  @Override
  default Context responseStream(@NonNull SneakyThrows.Consumer<OutputStream> consumer)
      throws Exception {
    try (OutputStream out = responseStream()) {
      consumer.accept(out);
    }
    return this;
  }

  @Override
  default PrintWriter responseWriter() {
    return responseWriter(MediaType.text);
  }

  @Override
  default Context responseWriter(@NonNull SneakyThrows.Consumer<PrintWriter> consumer)
      throws Exception {
    return responseWriter(MediaType.text, consumer);
  }

  @Override
  default Context responseWriter(
      @NonNull MediaType contentType, @NonNull SneakyThrows.Consumer<PrintWriter> consumer)
      throws Exception {
    try (PrintWriter writer = responseWriter(contentType)) {
      consumer.accept(writer);
    }
    return this;
  }

  @Override
  default Context sendRedirect(@NonNull String location) {
    return sendRedirect(StatusCode.FOUND, location);
  }

  @Override
  default Context sendRedirect(@NonNull StatusCode redirect, @NonNull String location) {
    setResponseHeader("location", location);
    return send(redirect);
  }

  @Override
  default Context send(@NonNull byte[]... data) {
    ByteBuffer[] buffer = new ByteBuffer[data.length];
    for (int i = 0; i < data.length; i++) {
      buffer[i] = ByteBuffer.wrap(data[i]);
    }
    return send(buffer);
  }

  @Override
  default Context send(@NonNull String data) {
    return send(data, StandardCharsets.UTF_8);
  }

  @Override
  default Context send(@NonNull FileDownload file) {
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

  @Override
  default Context send(@NonNull Path file) {
    try {
      setDefaultResponseType(MediaType.byFile(file));
      return send(FileChannel.open(file));
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override
  default Context sendError(@NonNull Throwable cause) {
    sendError(cause, getRouter().errorCode(cause));
    return this;
  }

  /**
   * Send an error response. This method set the error code.
   *
   * @param cause Error. If this is a fatal error it is going to be rethrow it.
   * @param code Default error code.
   * @return This context.
   */
  @Override
  default Context sendError(@NonNull Throwable cause, @NonNull StatusCode code) {
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
          log.debug(
              "error handler resulted in a exception while processing `{}`", cause.toString(), x);
        } else {
          log.error(
              "error handler resulted in a exception while processing `{}`", cause.toString(), x);
        }
      }
    }
    /* rethrow fatal exceptions: */
    if (SneakyThrows.isFatal(cause)) {
      throw SneakyThrows.propagate(cause);
    }
    return this;
  }

  @Override
  default OutputFactory getOutputFactory() {
    return getRouter().getOutputFactory().getContextFactory();
  }
}
