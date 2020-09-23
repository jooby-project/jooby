/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler.reactive;


import io.reactivex.rxjava3.observers.DefaultObserver;

public class RxObserver extends DefaultObserver<Object> {

  private ChunkedSubscriber subscriber;

  public RxObserver(ChunkedSubscriber subscriber) {
    this.subscriber = subscriber;
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

  @Override protected void onStart() {
    subscriber.onSubscribe(subcribe(this));
  }

  private static ChunkedSubscription subcribe(RxObserver rxObserver) {
    return new ChunkedSubscription() {
      @Override public void request(long n) {
        // NOOP
      }

      @Override public void cancel() {
        rxObserver.cancel();
      }
    };
  }

}
