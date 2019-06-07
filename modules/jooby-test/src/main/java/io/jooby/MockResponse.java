/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

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

  /**
   * Response headers.
   *
   * @return Response headers.
   */
  public @Nonnull Map<String, Object> getHeaders() {
    return headers == null ? Collections.emptyMap() : Collections.unmodifiableMap(headers);
  }

  /**
   * Set response headers.
   *
   * @param headers Response headers.
   * @return This response.
   */
  public @Nonnull MockResponse setHeaders(@Nonnull Map<String, Object> headers) {
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
  public @Nonnull MockResponse setHeader(@Nonnull String name, @Nonnull String value) {
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
  public @Nonnull MockResponse setHeader(@Nonnull String name, @Nonnull Object value) {
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
  public @Nonnull MockResponse setContentType(@Nonnull MediaType contentType) {
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
  public MockResponse setContentLength(long length) {
    this.length = length;
    headers.put("content-length", Long.toString(length));
    return this;
  }

  /**
   * Response status code.
   *
   * @return Response status code.
   */
  public StatusCode getStatusCode() {
    return statusCode;
  }

  /**
   * Set response status code.
   *
   * @param statusCode Response status code.
   * @return This response.
   */
  public MockResponse setStatusCode(@Nonnull StatusCode statusCode) {
    this.statusCode = statusCode;
    return this;
  }

  @Nonnull @Override public Object value() {
    return result;
  }

  /**
   * Set route response value.
   * @param result Route response value.
   * @return This response.
   */
  public MockResponse setResult(@Nonnull Object result) {
    this.result = result;
    return this;
  }
}
