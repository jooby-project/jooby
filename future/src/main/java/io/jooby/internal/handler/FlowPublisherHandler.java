package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.Route;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;

import javax.annotation.Nonnull;
import java.util.concurrent.Flow;

public class FlowPublisherHandler implements ChainedHandler {

  private final Route.Handler next;

  public FlowPublisherHandler(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
    try {
      Flow.Publisher result = (Flow.Publisher) next.apply(ctx);
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
