package io.jooby.internal.handler.reactive;

import java.util.concurrent.Flow;

public class JavaFlowSubscription implements ChunkedSubscription {

  private Flow.Subscription subscription;

  public JavaFlowSubscription(Flow.Subscription subscription) {
    this.subscription = subscription;
  }

  @Override public void request(long n) {
    subscription.request(n);
  }

  @Override public void cancel() {
    subscription.cancel();
  }
}
