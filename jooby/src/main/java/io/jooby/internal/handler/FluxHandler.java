package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.Route;
import io.reactivex.Flowable;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;

public class FluxHandler implements ChainedHandler {

  private final Route.Handler next;

  public FluxHandler(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
    try {
      Flux result = (Flux) next.apply(ctx);
      result.subscribe(new ContextSubscriber(ctx));
      return result;
    } catch (Throwable x) {
      ctx.sendError(x);
      return Flowable.error(x);
    }
  }

  @Override public Route.Handler next() {
    return next;
  }
}
