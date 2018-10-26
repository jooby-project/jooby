package io.jooby;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
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
    AtomicReference<String> cachedDate = new AtomicReference<>();
    Runnable CLEAR_DATE = () -> cachedDate.set(null);
    return next -> ctx -> setDate(ctx, cachedDate, CLEAR_DATE);
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
    AtomicReference<String> cachedDate = new AtomicReference<>();
    Runnable CLEAR_DATE = () -> cachedDate.set(null);
    return next -> ctx -> {
      ctx.header(H_SERVER, ctx.name());
      ctx.type(contentType);
      setDate(ctx, cachedDate, CLEAR_DATE);
      return next.apply(ctx);
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

  private static final Context setDate(Context ctx, AtomicReference<String> cachedDateString,
      Runnable clearTask) {
    String dateString = cachedDateString.get();
    if (dateString == null) {
      long realTime = System.currentTimeMillis();
      dateString = formatter.format(Instant.ofEpochMilli(realTime));
      if (cachedDateString.compareAndSet(null, dateString)) {
        ctx.io().executeAfter(clearTask, 1000, TimeUnit.MILLISECONDS);
      }
    }
    ctx.header(H_DATE, dateString);
    return ctx;
  }
}
