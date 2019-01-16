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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Collection of utility filters.
 */
public final class Decorators {
  private static class DateHeader {
    private volatile long nextUpdateTime = -1;
    private volatile String cachedDate;

    public String compute() {
      long time = System.currentTimeMillis();
      if (time > nextUpdateTime) {
        cachedDate = formatter.format(Instant.ofEpochMilli(time));
        nextUpdateTime = time + 1000;
      }
      return cachedDate;
    }
  }

  public static final String H_DATE = "Date";
  public static final String H_SERVER = "Server";

  private static final DateTimeFormatter formatter = Context.RFC1123;

  /**
   * A filter that set the `server` header.
   *
   * @return A filter that set the `server` header.
   */
  public static final Route.Decorator server() {
    return next -> ctx -> next.apply(ctx.header(H_SERVER, ctx.name()));
  }

  /**
   * A filter that set the `content-type` header.
   *
   * @param type Content-Type header.
   * @return A filter that set the `content-type` header.
   */
  public static final Route.Decorator contentType(@Nonnull String type) {
    return contentType(MediaType.valueOf(type));
  }

  /**
   * A filter that set the `content-type` header.
   *
   * @param type Content-Type header.
   * @return A filter that set the `content-type` header.
   */
  public static final Route.Decorator contentType(@Nonnull MediaType type) {
    return next -> ctx -> next.apply(ctx.type(type));
  }

  /**
   * A filter that set the `date` header.
   *
   * @return A filter that set the `date` header.
   */
  public static final Route.Decorator date() {
    DateHeader date = new DateHeader();
    return next -> ctx -> next.apply(ctx.header(H_DATE, date.compute()));
  }

  /**
   * A filter that set:
   *
   * - The `server` header.
   * - The `Content-Type` header.
   * - The `Date` header.
   *
   * @param contentType Content type.
   * @return Default headers filter.
   */
  public static final Route.Decorator defaultHeaders(@Nonnull MediaType contentType) {
    return defaultHeaders(contentType, contentType.charset());
  }

  /**
   * A filter that set:
   *
   * - The `server` header.
   * - The `Content-Type` header using a `text/plain` content type.
   * - The `Date` header.
   *
   * @return Default headers filter.
   */
  public static final Route.Decorator defaultHeaders() {
    return defaultHeaders(MediaType.text, StandardCharsets.UTF_8);
  }

  private static final Route.Decorator defaultHeaders(MediaType contentType, Charset charset) {
    DateHeader date = new DateHeader();
    return next -> ctx -> {
      ctx.header(H_SERVER, ctx.name());
      ctx.type(contentType, charset);
      ctx.header(H_DATE, date.compute());
      return next.apply(ctx);
    };
  }
}
