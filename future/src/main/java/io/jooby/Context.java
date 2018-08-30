package io.jooby;

import org.jooby.funzy.Throwing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public interface Context {

  /** 16KB constant. */
  int _16KB = 0x4000;

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

  /* **********************************************************************************************
   * Request Headers
   * **********************************************************************************************
   */

  @Nonnull default Value header(@Nonnull String name) {
    return headers().get(name);
  }

  @Nonnull Value headers();

  /* **********************************************************************************************
   * Form/Multipart methods
   * **********************************************************************************************
   */

  @Nonnull default Value form(@Nonnull String name) {
    return form().get(name);
  }

  @Nonnull Form form();

  @Nonnull default Value multipart(@Nonnull String name) {
    return multipart().get(name);
  }

  @Nonnull default List<Value.Upload> files(@Nonnull String name) {
    Value value = multipart(name);
    int len = value.size();
    List<Value.Upload> result = new ArrayList<>(len);
    for (int i = 0; i < len; i++) {
      result.add(value.get(i).upload());
    }
    return result;
  }

  @Nonnull default Value.Upload file(@Nonnull String name) {
    return multipart(name).upload();
  }

  /**
   * Parse a multipart/form-data request and returns the result.
   *
   *
   * <strong>NOTE:</strong> this method throws   an {@link IllegalStateException} when call it from
   * <code>IO thread</code>;
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

  default @Nonnull Context dispatch(@Nonnull Runnable action) {
    return dispatch(worker(), action);
  }

  @Nonnull Context dispatch(@Nonnull Executor executor, @Nonnull Runnable action);

  @Nonnull Context detach(@Nonnull Runnable action);

  @Nonnull Executor worker();

  @Nullable default <T> T get(String name) {
    return (T) locals().get(name);
  }

  @Nonnull default Context set(@Nonnull String name, @Nonnull Object value) {
    locals().put(name, value);
    return this;
  }

  @Nonnull Map<String, Object> locals();

  /**
   * **********************************************************************************************
   * **** Response methods *************************************************************************
   * **********************************************************************************************
   */

  @Nonnull default Context type(@Nonnull String contentType, @Nonnull Charset charset) {
    return type(contentType, charset.name());
  }

  @Nonnull default Context type(@Nonnull String contentType) {
    return type(contentType, StandardCharsets.UTF_8.name());
  }

  @Nonnull Context type(@Nonnull String contentType, String charset);

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
}
