package io.jooby;

import io.jooby.internal.UrlParser;
import org.jooby.funzy.Throwing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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

  /**
   * **********************************************************************************************
   * **** Request methods *************************************************************************
   * **********************************************************************************************
   */
  @Nonnull String method();

  @Nonnull Route route();

  /**
   * Request path without decoding (a.k.a raw Path). QueryString (if any) is not included.
   *
   * @return Request path without decoding (a.k.a raw Path). QueryString (if any) is not included.
   */
  @Nonnull String path();

  @Nonnull default Value param(@Nonnull String name) {
    String value = params().get(name);
    return value == null ?
        new Value.Missing(name) :
        new Value.Simple(name, UrlParser.decodePath(value));
  }

  @Nonnull Map<String, String> params();

  /* **********************************************************************************************
   * Query String methods
   * **********************************************************************************************
   */
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

  @Nonnull QueryString query();

  @Nonnull default <T> T query(Reified<T> type) {
    return query().to(type);
  }

  @Nonnull default <T> T query(Class<T> type) {
    return query().to(type);
  }

  /* **********************************************************************************************
   * Request Headers
   * **********************************************************************************************
   */

  @Nonnull default Value header(@Nonnull String name) {
    return headers().get(name);
  }

  @Nonnull Value headers();

  /* **********************************************************************************************
   * Formdata/Multipart methods
   * **********************************************************************************************
   */

  @Nonnull default Value form(@Nonnull String name) {
    return form().get(name);
  }

  @Nonnull Formdata form();

  @Nonnull default <T> T form(Reified<T> type) {
    return form().to(type);
  }

  @Nonnull default <T> T form(Class<T> type) {
    return form().to(type);
  }

  @Nonnull default Value multipart(@Nonnull String name) {
    return multipart().get(name);
  }

  @Nonnull default <T> T multipart(Reified<T> type) {
    return multipart().to(type);
  }

  @Nonnull default <T> T multipart(Class<T> type) {
    return multipart().to(type);
  }

  @Nonnull default List<Upload> files(@Nonnull String name) {
    Value multipart = multipart(name);
    List<Upload> result = new ArrayList<>();
    for (Value value : multipart) {
      result.add(value.upload());
    }
    return result;
  }

  @Nonnull default Upload file(@Nonnull String name) {
    return multipart(name).upload();
  }

  /**
   * Parse a multipart/form-data request and returns the result.
   *
   * <strong>NOTE:</strong> this method throws an {@link IllegalStateException} when call it from
   * <code>EVENT_LOOP thread</code>;
   *
   * @return Multipart node.
   */
  @Nonnull Multipart multipart();

  /* **********************************************************************************************
   * Request Body
   * **********************************************************************************************
   */

  default @Nonnull <T> T body(@Nonnull Class<T> type) {
    return body(Reified.get(type));
  }

  default @Nonnull <T> T body(@Nonnull Class<T> type, @Nonnull String contentType) {
    return body(Reified.get(type), contentType);
  }

  default @Nonnull <T> T body(@Nonnull Reified<T> type) {
    String contentType = header("Content-Type").value("text/plain");
    int i = contentType.indexOf(';');
    if (i > 0) {
      return body(type, contentType.substring(0, i));
    }
    return body(type, contentType);
  }

  default @Nonnull <T> T body(@Nonnull Reified<T> type, @Nonnull String contentType) {
    try {
      return parser(contentType).parse(this, type);
    } catch (Exception x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  @Nonnull Body body();

  /* **********************************************************************************************
   * Body Parser
   * **********************************************************************************************
   */
  @Nonnull Parser parser(@Nonnull String contentType);

  @Nonnull Context parser(@Nonnull String contentType, @Nonnull Parser parser);

  /* **********************************************************************************************
   * Dispatch methods
   * **********************************************************************************************
   */
  boolean isInIoThread();

  @Nonnull Context dispatch(@Nonnull Runnable action);

  @Nonnull Context dispatch(@Nonnull Executor executor, @Nonnull Runnable action);

  @Nonnull Context detach(@Nonnull Runnable action);

  @Nullable <T> T get(String name);

  @Nonnull Context set(@Nonnull String name, @Nonnull Object value);

  @Nonnull Map<String, Object> locals();

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

  @Nonnull Context length(long length);

  @Nonnull default Context type(@Nonnull MediaType contentType) {
    return type(contentType.value(), contentType.charset());
  }

  @Nonnull default Context type(@Nonnull String contentType) {
    return type(MediaType.valueOf(contentType));
  }

  @Nonnull Context type(@Nonnull String contentType, @Nullable String charset);

  @Nonnull default Context statusCode(StatusCode statusCode) {
    return statusCode(statusCode.value());
  }

  @Nonnull Context statusCode(int statusCode);

  @Nonnull Context send(@Nonnull Object result);

  default @Nonnull Context sendText(@Nonnull String data) {
    return sendText(data, StandardCharsets.UTF_8);
  }

  @Nonnull Context sendText(@Nonnull String data, @Nonnull Charset charset);

  @Nonnull Context sendBytes(@Nonnull byte[] data);

  @Nonnull Context sendBytes(@Nonnull ByteBuffer data);

  @Nonnull default Context sendStatusCode(StatusCode statusCode) {
    return sendStatusCode(statusCode.value());
  }

  @Nonnull Context sendStatusCode(int statusCode);

  @Nonnull Context sendError(@Nonnull Throwable cause);

  boolean isResponseStarted();

  void destroy();

  /**
   * Name of the underlying HTTP server: netty, utow, jetty, etc..
   *
   * @return Name of the underlying HTTP server: netty, utow, jetty, etc..
   */
  String name();
}
