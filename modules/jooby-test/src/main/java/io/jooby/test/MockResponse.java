/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.MediaType;
import io.jooby.StatusCode;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;

/**
 * Response generate by {@link MockRouter}. Contains all response metadata as well as route returns
 * value.
 *
 * App.java
 * <pre>{@code
 * {
 *
 *   get("/", ctx -> "OK");
 *
 * }
 * }</pre>
 *
 * UnitTest:
 * <pre>{@code
 *   MockRouter router = new MockRouter(new App());
 *
 *   router.get("/", response -> {
 *
 *     assertEquals("OK", response.getResult());
 *
 *   });
 * }</pre>
 *
 * @author edgar
 * @since 2.0.0
 */
public class MockResponse implements MockValue {

  private Object result;

  private StatusCode statusCode = StatusCode.OK;

  private Map<String, Object> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

  private MediaType contentType;

  private long length = -1;

  private CountDownLatch latch = new CountDownLatch(1);

  /**
   * Response headers.
   *
   * @return Response headers.
   */
  public @NonNull Map<String, Object> getHeaders() {
    return headers == null ? Collections.emptyMap() : Collections.unmodifiableMap(headers);
  }

  /**
   * Set response headers.
   *
   * @param headers Response headers.
   * @return This response.
   */
  public @NonNull MockResponse setHeaders(@NonNull Map<String, Object> headers) {
    headers.forEach(this::setHeader);
    return this;
  }

  /**
   * Set response header.
   *
   * @param name Header name.
   * @param value Header value.
   * @return This response.
   */
  public @NonNull MockResponse setHeader(@NonNull String name, @NonNull String value) {
    if ("content-type".equalsIgnoreCase(name)) {
      setContentType(MediaType.valueOf(value));
    } else if ("content-length".equalsIgnoreCase(name)) {
      setContentLength(Long.parseLong(value));
    } else {
      this.headers.put(name, value);
    }
    return this;
  }

  /**
   * Set response header.
   *
   * @param name Header name.
   * @param value Header value.
   * @return This response.
   */
  public @NonNull MockResponse setHeader(@NonNull String name, @NonNull Object value) {
    return setHeader(name, value.toString());
  }

  /**
   * Response content type.
   *
   * @return Response content type.
   */
  public @Nullable MediaType getContentType() {
    return contentType == null ? MediaType.text : contentType;
  }

  /**
   * Set response content type.
   *
   * @param contentType Response content type.
   * @return This response.
   */
  public @NonNull MockResponse setContentType(@NonNull MediaType contentType) {
    this.contentType = contentType;
    headers.put("content-type", contentType.toContentTypeHeader(contentType.getCharset()));
    return this;
  }

  /**
   * Response content length.
   *
   * @return Response content length.
   */
  public long getContentLength() {
    return length;
  }

  /**
   * Set response content length.
   *
   * @param length Response content length.
   * @return This response.
   */
  public @NonNull MockResponse setContentLength(long length) {
    this.length = length;
    headers.put("content-length", Long.toString(length));
    return this;
  }

  /**
   * Response status code.
   *
   * @return Response status code.
   */
  public @NonNull StatusCode getStatusCode() {
    return statusCode;
  }

  /**
   * Set response status code.
   *
   * @param statusCode Response status code.
   * @return This response.
   */
  public @NonNull MockResponse setStatusCode(@NonNull StatusCode statusCode) {
    this.statusCode = statusCode;
    return this;
  }

  @Nullable @Override public Object value() {
    return result;
  }

  /**
   * Set route response value.
   * @param result Route response value.
   * @return This response.
   */
  public @NonNull MockResponse setResult(@Nullable Object result) {
    this.result = result;
    latch.countDown();
    return this;
  }

  CountDownLatch getLatch() {
    return latch;
  }
}
