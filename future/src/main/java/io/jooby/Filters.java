package io.jooby;

import io.undertow.util.DateUtils;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Collection of utility filters.
 */
public final class Filters {
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
    return new Route.Filter() {
      volatile long nextUpdateTime = -1;
      volatile String cachedDate;

      @Nonnull @Override public Route.Handler apply(@Nonnull Route.Handler next) {
        return ctx -> {
          long time = System.currentTimeMillis();
          if (time > nextUpdateTime) {
            cachedDate = formatter.format(Instant.ofEpochMilli(time));
            nextUpdateTime = time + 1000;
          }
          ctx.header(H_DATE, cachedDate);
          return next.apply(ctx);
        };
      }
    };
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
    Route.Filter date = date();
    return next -> ctx -> {
      ctx.header(H_SERVER, ctx.name());
      ctx.type(contentType);
      return date.apply(next).apply(ctx);
    };
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
    return defaultHeaders(MediaType.text);
  }
}
