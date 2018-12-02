package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.Route;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.observers.DisposableMaybeObserver;

import javax.annotation.Nonnull;

public class MaybeHandler implements ChainedHandler {

  private final Route.Handler next;

  public MaybeHandler(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
    try {
      Maybe result = (Maybe) next.apply(ctx);
      result.subscribe(new DisposableMaybeObserver() {
        @Override public void onSuccess(Object value) {
          ctx.send(value);
        }

        @Override public void onError(Throwable e) {
          ctx.sendError(e);
        }

        @Override public void onComplete() {

        }
      });
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
