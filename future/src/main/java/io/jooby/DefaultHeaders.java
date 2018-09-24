package io.jooby;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class DefaultHeaders implements Route.Filter {
  private static final String DATE = "Date";
  private static final String SERVER = "Server";
  private static final DateTimeFormatter formatter = Context.RFC1123;

  private volatile long nextUpdateTime = -1;
  private volatile String cachedDate;
  private Consumer<Context> headers;

  public DefaultHeaders set(@Nonnull Consumer<Context> headers) {
    this.headers = headers;
    return this;
  }

  @Nonnull @Override public Route.Handler apply(@Nonnull Route.Handler next) {
    return ctx -> {
      long time = System.currentTimeMillis();
      if (time < nextUpdateTime) {
        ctx.header(DATE, cachedDate);
      } else {
        nextUpdateTime = time + 1000;
        cachedDate = formatter.format(Instant.ofEpochMilli(time));
        ctx.header(DATE, cachedDate);
      }
      ctx.header(SERVER, ctx.name());
      if (headers != null) {
        headers.accept(ctx);
      }
      return next.apply(ctx);
    };
  }

}
