/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.Route;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.observers.DefaultObserver;

import javax.annotation.Nonnull;

public class ObservableHandler implements ChainedHandler {

  private final Route.Handler next;

  public ObservableHandler(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
    try {
      Observable result = (Observable) next.apply(ctx);
      result.subscribe(new DefaultObserver() {
        @Override public void onNext(Object value) {
          ctx.render(value);
        }

        @Override public void onError(Throwable e) {
          ctx.sendError(e);
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

  @Override public Route.Handler next() {
    return next;
  }
}
