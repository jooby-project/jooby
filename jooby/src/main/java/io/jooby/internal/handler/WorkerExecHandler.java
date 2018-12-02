package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.Route;

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;

public class WorkerExecHandler implements ChainedHandler {
  private final Route.Handler next;
  private final Executor executor;

  public WorkerExecHandler(Route.Handler next, Executor executor) {
    this.next = next;
    this.executor = executor;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
    return ctx.dispatch(executor, () -> next.execute(ctx));
  }

  @Override public Route.Handler next() {
    return next;
  }
}
