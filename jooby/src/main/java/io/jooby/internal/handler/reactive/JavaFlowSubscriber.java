package io.jooby.internal.handler.reactive;

import java.util.concurrent.Flow;

public class JavaFlowSubscriber implements Flow.Subscriber<Object> {

  private final ChunkedSubscriber subscriber;

  public JavaFlowSubscriber(ChunkedSubscriber subscriber) {
    this.subscriber = subscriber;
  }

  @Override public void onSubscribe(Flow.Subscription s) {
    subscriber.onSubscribe(new JavaFlowSubscription(s));
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
