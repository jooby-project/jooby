/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.rxjava3;

import java.util.concurrent.CompletionException;

import org.slf4j.Logger;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.StatusCode;
import io.reactivex.rxjava3.core.MaybeObserver;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;

public class RxSubscriber implements MaybeObserver<Object>, SingleObserver<Object> {

  private final Context ctx;

  private Disposable subscription;

  public RxSubscriber(Context ctx) {
    this.ctx = ctx;
  }

  @Override
  public void onSubscribe(Disposable d) {
    this.subscription = d;
  }

  @Override
  public void onSuccess(Object value) {
    after(ctx, value, null);
    // See https://github.com/jooby-project/jooby/issues/3486
    if (!ctx.isResponseStarted() && value != ctx) {
      ctx.render(value);
    }
  }

  @Override
  public void onError(Throwable x) {
    after(ctx, null, unwrap(x));
    ctx.sendError(x);
    subscription.dispose();
  }

  private Throwable unwrap(Throwable x) {
    if (x instanceof CompletionException && x.getCause() != null) {
      return x.getCause();
    }
    return x;
  }

  @Override
  public void onComplete() {
    if (!ctx.isResponseStarted()) {
      // assume it is a maybe response:
      ctx.send(StatusCode.NOT_FOUND);
    }
    subscription.dispose();
  }

  private void after(Context ctx, Object value, Throwable failure) {
    Route.After after = ctx.getRoute().getAfter();
    if (after != null) {
      try {
        after.apply(ctx, value, failure);
      } catch (Exception unexpected) {
        Logger log = ctx.getRouter().getLog();
        log.debug("After invocation resulted in exception", unexpected);
      }
    }
  }
}
