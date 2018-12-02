package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.Route;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;

import javax.annotation.Nonnull;

public class PublisherHandler implements ChainedHandler {

  private final Route.Handler next;

  public PublisherHandler(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
    try {
      Publisher result = (Publisher) next.apply(ctx);
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
