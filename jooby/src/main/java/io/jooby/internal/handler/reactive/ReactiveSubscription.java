package io.jooby.internal.handler.reactive;

import org.reactivestreams.Subscription;

public class ReactiveSubscription implements ChunkedSubscription {

  private Subscription subscription;

  public ReactiveSubscription(Subscription subscription) {
    this.subscription = subscription;
  }

  @Override public void request(long n) {
    subscription.request(n);
  }

  @Override public void cancel() {
    subscription.cancel();
  }
}
