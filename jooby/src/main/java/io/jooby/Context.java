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

import io.jooby.internal.UrlParser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
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

public interface Context {

  String ACCEPT = "Accept";

  ZoneId GMT = ZoneId.of("GMT");

  String RFC1123_PATTERN = "EEE, dd MMM yyyy HH:mm:ss z";

  DateTimeFormatter RFC1123 = DateTimeFormatter
      .ofPattern(RFC1123_PATTERN, Locale.US)
      .withZone(GMT);

  /**
   * **********************************************************************************************
   * **** Native methods *************************************************************************
   * **********************************************************************************************
   */

  @Nonnull Router router();

  /**
   * **********************************************************************************************
   * **** Request methods *************************************************************************
   * **********************************************************************************************
   */
  @Nonnull String method();

  @Nonnull Route route();

  @Nonnull Context route(@Nonnull Route route);

  /**
   * Request path without decoding (a.k.a raw Path). QueryString (if any) is not included.
   *
   * @return Request path without decoding (a.k.a raw Path). QueryString (if any) is not included.
   */
  @Nonnull String pathString();

  @Nonnull default Value path(@Nonnull String name) {
    String value = pathMap().get(name);
    return value == null ?
        new Value.Missing(name) :
        new Value.Simple(name, UrlParser.decodePath(value));
  }

  @Nonnull default Value path() {
    return Value.path(pathMap());
  }

  @Nonnull Map<String, String> pathMap();

  @Nonnull Context pathMap(@Nonnull Map<String, String> pathMap);

  /* **********************************************************************************************
   * Query String API
   * **********************************************************************************************
   */
  @Nonnull QueryString query();

  @Nonnull default Value query(@Nonnull String name) {
    return query().get(name);
  }

  /**
   * Query string with the leading <code>?</code> or empty string.
   *
   * @return Query string with the leading <code>?</code> or empty string.
   */
  @Nonnull default String queryString() {
    return query().queryString();
  }

  @Nonnull default <T> T query(Reified<T> type) {
    return query().to(type);
  }

  @Nonnull default <T> T query(Class<T> type) {
    return query().to(type);
  }

  @Nonnull default Map<String, String> queryMap() {
    return query().toMap();
  }

  @Nonnull default Map<String, List<String>> queryMultimap() {
    return query().toMultimap();
  }

  /* **********************************************************************************************
   * Header API
   * **********************************************************************************************
   */

  @Nonnull Value headers();

  @Nonnull default Value header(@Nonnull String name) {
    return headers().get(name);
  }

  @Nonnull default Map<String, String> headerMap() {
    return headers().toMap();
  }

  @Nonnull default Map<String, List<String>> headerMultimap() {
    return headers().toMultimap();
  }

  default boolean accept(MediaType contentType) {
    String accept = header(ACCEPT).value(MediaType.ALL);
    return contentType.matches(accept);
  }

  @Nonnull default MediaType requestType() {
    return header("Content-Type").toOptional().map(MediaType::valueOf).orElse(null);
  }

  default long requestLength() {
    return header("Content-Length").longValue(-1);
  }

  @Nonnull String remoteAddress();

  default @Nonnull String hostname() {
    return header("host").toOptional()
        .map(host -> {
          int index = host.indexOf(':');
          return index > 0 ? host.substring(0, index) : host;
        })
        .orElse(remoteAddress());
  }

  @Nonnull String protocol();

  /* **********************************************************************************************
   * Form API
   * **********************************************************************************************
   */

  @Nonnull Formdata form();

  @Nonnull default Map<String, List<String>> formMultimap() {
    return form().toMultimap();
  }

  @Nonnull default Map<String, String> formMap() {
    return form().toMap();
  }

  @Nonnull default Value form(@Nonnull String name) {
    return form().get(name);
  }

  @Nonnull default <T> T form(Reified<T> type) {
    return form().to(type);
  }

  @Nonnull default <T> T form(Class<T> type) {
    return form().to(type);
  }

  /* **********************************************************************************************
   * Multipart API
   * **********************************************************************************************
   */

  /**
   * Parse a multipart/form-data request and returns the result.
   *
   * <strong>NOTE:</strong> this method throws an {@link IllegalStateException} when call it from
   * <code>EVENT_LOOP thread</code>;
   *
   * @return Multipart node.
   */
  @Nonnull Multipart multipart();

  @Nonnull default Value multipart(@Nonnull String name) {
    return multipart().get(name);
  }

  @Nonnull default <T> T multipart(Reified<T> type) {
    return multipart().to(type);
  }

  @Nonnull default <T> T multipart(Class<T> type) {
    return multipart().to(type);
  }

  @Nonnull default Map<String, List<String>> multipartMultimap() {
    return multipart().toMultimap();
  }

  @Nonnull default Map<String, String> multipartMap() {
    return multipart().toMap();
  }

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

  @Nonnull default List<FileUpload> files(@Nonnull String name) {
    Value multipart = multipart(name);
    List<FileUpload> result = new ArrayList<>();
    for (Value value : multipart) {
      result.add(value.fileUpload());
    }
    return result;
  }

  @Nonnull default FileUpload file(@Nonnull String name) {
    return multipart(name).fileUpload();
  }

  /* **********************************************************************************************
   * Request Body
   * **********************************************************************************************
   */

  @Nonnull Body body();

  default @Nonnull <T> T body(@Nonnull Reified<T> type) {
    return body(type.getType());
  }

  default @Nonnull <T> T body(@Nonnull Reified<T> type, @Nonnull MediaType contentType) {
    return body(type.getType(), contentType);
  }

  default @Nonnull <T> T body(@Nonnull Type type) {
    MediaType contentType = MediaType.valueOf(header("Content-Type")
        .value("text/plain"));
    return body(type, contentType);
  }

  default @Nonnull <T> T body(@Nonnull Type type, @Nonnull MediaType contentType) {
    try {
      return parser(contentType).parse(this, type);
    } catch (Exception x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  /* **********************************************************************************************
   * Body Parser
   * **********************************************************************************************
   */
  default @Nonnull Parser parser(@Nonnull MediaType contentType) {
    return route().parser(contentType);
  }

  /* **********************************************************************************************
   * Dispatch methods
   * **********************************************************************************************
   */
  boolean isInIoThread();

  @Nonnull Context dispatch(@Nonnull Runnable action);

  @Nonnull Context dispatch(@Nonnull Executor executor, @Nonnull Runnable action);

  @Nonnull Context detach(@Nonnull Runnable action);

  @Nullable <T> T attribute(String name);

  @Nonnull Context attribute(@Nonnull String name, @Nonnull Object value);

  @Nonnull Map<String, Object> attributes();

  /**
   * **********************************************************************************************
   * **** Response methods *************************************************************************
   * **********************************************************************************************
   */

  @Nonnull default Context header(@Nonnull String name, @Nonnull Date value) {
    return header(name, RFC1123.format(Instant.ofEpochMilli(value.getTime())));
  }

  @Nonnull default Context header(@Nonnull String name, @Nonnull Instant value) {
    return header(name, RFC1123.format(value));
  }

  @Nonnull default Context header(@Nonnull String name, @Nonnull Object value) {
    if (value instanceof Date) {
      return header(name, (Date) value);
    }
    if (value instanceof Instant) {
      return header(name, (Instant) value);
    }
    return header(name, value.toString());
  }

  @Nonnull Context header(@Nonnull String name, @Nonnull String value);

  @Nonnull Context responseLength(long length);

  @Nonnull default Context responseType(@Nonnull MediaType contentType) {
    return responseType(contentType, contentType.charset());
  }

  @Nonnull Context defaultResponseType(@Nonnull MediaType contentType);

  @Nonnull Context responseType(@Nonnull MediaType contentType, @Nullable Charset charset);

  @Nonnull MediaType responseType();

  @Nonnull default Context statusCode(StatusCode statusCode) {
    return statusCode(statusCode.value());
  }

  @Nonnull Context statusCode(int statusCode);

  default @Nonnull Context render(@Nonnull Object result) {
    try {
      byte[] encode = route().renderer().encode(this, result);
      sendBytes(encode);
      return this;
    } catch (Exception x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  @Nonnull OutputStream responseStream();

  default @Nonnull OutputStream responseStream(MediaType contentType) {
    responseType(contentType);
    return responseStream();
  }

  default @Nonnull Context responseStream(MediaType contentType,
      Throwing.Consumer<OutputStream> consumer) throws Exception {
    responseType(contentType);
    return responseStream(consumer);
  }

  default @Nonnull Context responseStream(Throwing.Consumer<OutputStream> consumer)
      throws Exception {
    try (OutputStream out = responseStream()) {
      consumer.accept(out);
    }
    return this;
  }

  @Nonnull Sender responseSender();

  default @Nonnull Writer responseWriter() {
    return responseWriter(MediaType.text);
  }

  default @Nonnull Writer responseWriter(MediaType contentType) {
    return responseWriter(contentType, contentType.charset());
  }

  @Nonnull Writer responseWriter(MediaType contentType, Charset charset);

  default @Nonnull Context responseWriter(Throwing.Consumer<Writer> consumer) throws Exception {
    return responseWriter(MediaType.text, consumer);
  }

  default @Nonnull Context responseWriter(MediaType contentType, Throwing.Consumer<Writer> consumer)
      throws Exception {
    return responseWriter(contentType, contentType.charset(), consumer);
  }

  default @Nonnull Context responseWriter(MediaType contentType, Charset charset,
      Throwing.Consumer<Writer> consumer) throws Exception {
    try (Writer writer = responseWriter(contentType, charset)) {
      consumer.accept(writer);
    }
    return this;
  }

  default @Nonnull Context sendRedirect(@Nonnull String location) {
    return sendRedirect(StatusCode.FOUND, location);
  }

  default @Nonnull Context sendRedirect(@Nonnull StatusCode redirect, @Nonnull String location) {
    header("location", location);
    sendStatusCode(redirect);
    return this;
  }

  default @Nonnull Context sendString(@Nonnull String data) {
    return sendString(data, StandardCharsets.UTF_8);
  }

  @Nonnull Context sendString(@Nonnull String data, @Nonnull Charset charset);

  @Nonnull Context sendBytes(@Nonnull byte[] data);

  @Nonnull Context sendBytes(@Nonnull ByteBuffer data);

  @Nonnull Context sendStream(@Nonnull InputStream input);

  default @Nonnull Context sendFile(@Nonnull Path file) {
    try {
      return sendFile(FileChannel.open(file));
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  @Nonnull Context sendFile(@Nonnull FileChannel file);

  @Nonnull default Context sendStatusCode(StatusCode statusCode) {
    return sendStatusCode(statusCode.value());
  }

  @Nonnull Context sendStatusCode(int statusCode);

  @Nonnull default Context sendError(@Nonnull Throwable cause) {
    sendError(cause, router().errorCode(cause));
    if (Throwing.isFatal(cause)) {
      throw Throwing.sneakyThrow(cause);
    }
    return this;
  }

  @Nonnull default Context sendError(@Nonnull Throwable cause, StatusCode statusCode) {
    router().errorHandler().apply(this, cause, statusCode);
    return this;
  }

  boolean isResponseStarted();

  /**
   * Name of the underlying HTTP server: netty, utow, jetty, etc..
   *
   * @return Name of the underlying HTTP server: netty, utow, jetty, etc..
   */
  String name();
}
