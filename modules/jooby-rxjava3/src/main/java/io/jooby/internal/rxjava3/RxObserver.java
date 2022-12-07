/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.rxjava3;

import java.util.concurrent.Flow;

import io.reactivex.rxjava3.observers.DefaultObserver;

public class RxObserver extends DefaultObserver<Object> {

  private Flow.Subscriber subscriber;

  public RxObserver(Flow.Subscriber subscriber) {
    this.subscriber = subscriber;
  }

  @Override
  public void onNext(Object item) {
    subscriber.onNext(item);
  }

  @Override
  public void onError(Throwable x) {
    subscriber.onError(x);
  }

  @Override
  public void onComplete() {
    subscriber.onComplete();
  }

  @Override
  protected void onStart() {
    subscriber.onSubscribe(subcribe(this));
  }

  private static Flow.Subscription subcribe(RxObserver rxObserver) {
    return new Flow.Subscription() {
      @Override
      public void request(long n) {
        // NOOP
      }

      @Override
      public void cancel() {
        rxObserver.cancel();
      }
    };
  }
}
