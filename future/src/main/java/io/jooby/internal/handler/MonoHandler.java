package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.Route;
import io.reactivex.Flowable;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

public class MonoHandler implements ChainedHandler {

  private final Route.Handler next;

  public MonoHandler(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
    try {
      Mono result = (Mono) next.apply(ctx);
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
