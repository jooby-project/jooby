package io.jooby;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Collection of utility filters.
 */
public final class Filters {
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
  public static final Route.Filter server() {
    return next -> ctx -> next.apply(ctx.header(H_SERVER, ctx.name()));
  }

  /**
   * A filter that set the `content-type` header.
   *
   * @param type Content-Type header.
   * @return A filter that set the `content-type` header.
   */
  public static final Route.Filter contentType(@Nonnull String type) {
    return contentType(MediaType.valueOf(type));
  }

  /**
   * A filter that set the `content-type` header.
   *
   * @param type Content-Type header.
   * @return A filter that set the `content-type` header.
   */
  public static final Route.Filter contentType(@Nonnull MediaType type) {
    return next -> ctx -> next.apply(ctx.type(type));
  }

  /**
   * A filter that set the `date` header.
   *
   * @return A filter that set the `date` header.
   */
  public static final Route.Filter date() {
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
  public static final Route.Filter defaultHeaders(@Nonnull MediaType contentType) {
    return defaultHeaders(contentType.toString(), contentType.charset());
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
  public static final Route.Filter defaultHeaders() {
    return defaultHeaders("text/plain", null);
  }

  private static final Route.Filter defaultHeaders(String contentType, String charset) {
    DateHeader date = new DateHeader();
    return next -> ctx -> {
      ctx.header(H_SERVER, ctx.name());
      ctx.type(contentType, charset);
      ctx.header(H_DATE, date.compute());
      return next.apply(ctx);
    };
  }
}
