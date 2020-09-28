/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler.reactive;

import io.jooby.Context;
import io.jooby.StatusCode;
import io.reactivex.rxjava3.core.MaybeObserver;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;

public class RxSubscriber implements MaybeObserver<Object>, SingleObserver<Object> {

  private final Context context;

  private Disposable subscription;

  public RxSubscriber(Context context) {
    this.context = context;
  }

  @Override public void onSubscribe(Disposable d) {
    this.subscription = d;
  }

  @Override public void onSuccess(Object value) {
    context.render(value);
  }

  @Override public void onError(Throwable x) {
    context.sendError(x);
    subscription.dispose();
  }

  @Override public void onComplete() {
    if (!context.isResponseStarted()) {
      // assume it is a maybe response:
      context.send(StatusCode.NOT_FOUND);
    }
    subscription.dispose();
  }
}
