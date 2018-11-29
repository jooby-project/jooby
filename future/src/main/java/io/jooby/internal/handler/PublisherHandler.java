package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.Route;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import javax.annotation.Nonnull;

public class PublisherHandler implements Route.Handler {

  private final Route.Handler next;

  public PublisherHandler(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
    try {
      Publisher result = (Publisher) next.apply(ctx);
      result.subscribe(new Subscriber() {
        @Override public void onSubscribe(Subscription s) {
          s.request(Long.MAX_VALUE);
        }

        @Override public void onNext(Object value) {
          ctx.send(value);
        }

        @Override public void onError(Throwable x) {
          ctx.sendError(x);
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
}
