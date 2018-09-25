package io.jooby;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class DefaultHeaders implements Route.Filter {
  private static final String DATE = "Date";
  private static final String SERVER = "Server";
  private static final DateTimeFormatter formatter = Context.RFC1123;

  private static final AtomicReference<String> cachedDateString = new AtomicReference<>();
  private static final Runnable CLEAR_DATE = () -> cachedDateString.set(null);

  private Consumer<Context> headers;

  public DefaultHeaders set(@Nonnull Consumer<Context> headers) {
    this.headers = headers;
    return this;
  }

  @Nonnull @Override public Route.Handler apply(@Nonnull Route.Handler next) {
    return ctx -> {
      String dateString = cachedDateString.get();
      if (dateString == null) {
        //set the time and register a timer to invalidate it
        //note that this is racey, it does not matter if multiple threads do this
        //the perf cost of synchronizing would be more than the perf cost of multiple threads running it
        long realTime = System.currentTimeMillis();
        dateString = formatter.format(Instant.ofEpochMilli(realTime));
        if (cachedDateString.compareAndSet(null, dateString)) {
          ctx.io().executeAfter(CLEAR_DATE, 1000, TimeUnit.MILLISECONDS);
        }
      }
      ctx.header(DATE, dateString);
      ctx.header(SERVER, ctx.name());
      if (headers != null) {
        headers.accept(ctx);
      }
      return next.apply(ctx);
    };
  }

}
