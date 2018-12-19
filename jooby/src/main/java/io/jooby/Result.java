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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Result {

  private final Object value;

  private final int statusCode;

  private Map<String, String> headers;

  public Result(Object value, int statusCode) {
    this.value = value;
    this.statusCode = statusCode;
  }

  public Result(Object value, StatusCode statusCode) {
    this(value, statusCode.value());
  }

  public Map<String, String> headers() {
    return headers == null ? Collections.emptyMap() : headers;
  }

  public Result headers(Map<String, String> headers) {
    this.headers = new LinkedHashMap<>();
    headers.forEach((k, v) -> this.headers.put(k.toLowerCase(), v));
    return this;
  }

  public Result header(@Nonnull String name, @Nonnull String value) {
    if (headers == null) {
      headers = new LinkedHashMap<>();
    }
    this.headers.put(name.toLowerCase(), value);
    return this;
  }

  public String contentType() {
    return headers().get("content-type");
  }

  public long contentLength() {
    String len = headers().get("content-length");
    return len == null ? -1 : Long.parseLong(len);
  }

  public int statusCode() {
    return statusCode;
  }

  public Object value() {
    return value;
  }
}
