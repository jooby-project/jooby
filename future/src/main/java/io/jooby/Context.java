package io.jooby;

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
  @Nonnull Route.Filter gzip();

  /**
   * **********************************************************************************************
   * **** Request methods *************************************************************************
   * **********************************************************************************************
   */
  @Nonnull default String method() {
    return route().method();
  }

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

  @Nonnull default Map<String, String> params() {
    return route().params();
  }

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
   * Dispatch methods
   * **********************************************************************************************
   */
  boolean isInIoThread();

  default @Nonnull Context dispatch(@Nonnull Runnable action) {
    return dispatch(worker(), action);
  }

  @Nonnull Context dispatch(@Nonnull Executor executor, @Nonnull Runnable action);

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

  @Nonnull Context type(@Nonnull String contentType);

  @Nonnull default Context statusCode(StatusCode statusCode) {
    return statusCode(statusCode.value());
  }

  @Nonnull Context statusCode(int statusCode);

  default @Nonnull Context send(@Nonnull String data) {
    return send(data, StandardCharsets.UTF_8);
  }

  @Nonnull default Context send(@Nonnull String data, @Nonnull Charset charset) {
    return send(data.getBytes(charset));
  }

  @Nonnull default Context send(@Nonnull byte[] data) {
    return send(ByteBuffer.wrap(data));
  }

  @Nonnull Context send(@Nonnull ByteBuffer data);

  @Nonnull default Context sendStatusCode(StatusCode statusCode) {
    return sendStatusCode(statusCode.value());
  }

  @Nonnull Context sendStatusCode(int statusCode);

  boolean isResponseStarted();

  void destroy();
}
