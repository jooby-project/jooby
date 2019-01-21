package io.jooby.internal.handler.reactive;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class ReactiveSubscriber implements Subscriber<Object> {

  private final ChunkedSubscriber subscriber;

  public ReactiveSubscriber(ChunkedSubscriber subscriber) {
    this.subscriber = subscriber;
  }

  @Override public void onSubscribe(Subscription s) {
    subscriber.onSubscribe(new ReactiveSubscription(s));
  }

  @Override public void onNext(Object item) {
    subscriber.onNext(item);
  }

  @Override public void onError(Throwable x) {
    subscriber.onError(x);
  }

  @Override public void onComplete() {
    subscriber.onComplete();
  }
}
