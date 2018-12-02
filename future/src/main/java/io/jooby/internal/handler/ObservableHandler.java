package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.Route;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.observers.DefaultObserver;

import javax.annotation.Nonnull;

public class ObservableHandler implements ChainedHandler {

  private final Route.Handler next;

  public ObservableHandler(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
    try {
      Observable result = (Observable) next.apply(ctx);
      result.subscribe(new DefaultObserver() {
        @Override public void onNext(Object value) {
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
