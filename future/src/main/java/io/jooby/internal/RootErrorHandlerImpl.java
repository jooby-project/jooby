package io.jooby.internal;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.StatusCode;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.function.Function;

public class RootErrorHandlerImpl implements Route.RootErrorHandler {
  private final Route.ErrorHandler next;
  private final Function<Throwable, StatusCode> statusCode;
  private final Logger log;

  public RootErrorHandlerImpl(Route.ErrorHandler next, Logger log, Function<Throwable, StatusCode> statusCode) {
    this.next = next;
    this.log = log;
    this.statusCode = statusCode;
  }

  @Override public void apply(@Nonnull Context ctx, @Nonnull Throwable cause) {
    if (ctx.isResponseStarted()) {
     log.error("execution resulted in exception and response was already sent", cause);
    } else {
      next.apply(ctx, cause, statusCode.apply(cause));
    }
  }
}
