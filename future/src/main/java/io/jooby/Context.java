package io.jooby;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Executor;
import java.util.function.Function;

public interface Context {
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

  @Nonnull default Mutant param(@Nonnull String name) {
    String value = params().get(name);
    return () -> Collections.singletonList(value);
  }

  @Nonnull default Map<String, String> params() {
    return route().params();
  }

  boolean isInIoThread();

  default @Nonnull Context dispatch(@Nonnull Runnable action) {
    return dispatch(worker(), action);
  }

  @Nonnull Context dispatch(@Nonnull Executor executor, @Nonnull Runnable action);

  @Nonnull Executor worker();

  @Nullable default <T> T get(String name) {
    return  (T) locals().get(name);
  }

  @Nonnull default Context set(@Nonnull String name, @Nonnull Object value) {
    locals().put(name, value);
    return  this;
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
}
