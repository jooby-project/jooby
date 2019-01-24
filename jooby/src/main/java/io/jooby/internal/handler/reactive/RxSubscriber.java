/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler.reactive;

import io.jooby.Context;
import io.jooby.StatusCode;
import io.reactivex.MaybeObserver;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

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
      context.sendStatusCode(StatusCode.NOT_FOUND);
    }
    subscription.dispose();
  }
}
