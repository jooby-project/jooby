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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class MockResponse {

  private final Object value;

  private final StatusCode statusCode;

  private Map<String, String> headers = new LinkedHashMap<>();

  private MediaType contentType;

  private Charset charset = StandardCharsets.UTF_8;

  private long length = -1;

  public MockResponse(Object value, StatusCode statusCode) {
    this.value = value;
    this.statusCode = statusCode;
  }

  public @Nonnull Map<String, String> headers() {
    return headers == null ? Collections.emptyMap() : Collections.unmodifiableMap(headers);
  }

  public @Nonnull MockResponse headers(@Nonnull Map<String, String> headers) {
    headers.forEach(this::header);
    return this;
  }

  public @Nonnull MockResponse header(@Nonnull String name, @Nonnull String value) {
    if ("content-type".equalsIgnoreCase(name)) {
      setContentType(MediaType.valueOf(value));
    } else if ("content-length".equalsIgnoreCase(name)) {
      setContentLength(Long.parseLong(value));
    } else {
      this.headers.put(name.toLowerCase(), value);
    }
    return this;
  }

  public @Nullable MediaType getContentType() {
    return contentType == null ? MediaType.text : contentType;
  }

  public @Nonnull MockResponse setContentType(@Nonnull MediaType contentType) {
    return setContentType(contentType, contentType.getCharset());
  }

  public @Nonnull MockResponse setContentType(@Nonnull MediaType contentType, @Nullable Charset charset) {
    this.contentType = contentType;
    this.charset = charset;
    headers.put("content-type", contentType.toContentTypeHeader(charset));
    return this;
  }

  public @Nullable Charset getCharset() {
    return charset;
  }

  public long getContentLength() {
    return length;
  }

  public MockResponse setContentLength(long length) {
    this.length = length;
    headers.put("content-length", Long.toString(length));
    return this;
  }

  public StatusCode getStatusCode() {
    return statusCode;
  }

  public Object getValue() {
    return value;
  }
}
