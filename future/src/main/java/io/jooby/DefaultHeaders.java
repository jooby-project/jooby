package io.jooby;

import javax.annotation.Nonnull;
import java.time.Instant;

public class DefaultHeaders implements Route.Filter {
  private String server = "Jooby";

  @Nonnull @Override public Route.Handler apply(@Nonnull Route.Handler next) {
    return ctx -> {
      ctx.header("Date", Instant.now());
      ctx.header("Server", server);
      return next.apply(ctx);
    };
  }
}
