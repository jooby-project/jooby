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
package io.jooby.internal.handler.reactive;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.internal.handler.NextHandler;
import io.reactivex.Single;

import javax.annotation.Nonnull;

public class RxSingleHandler implements NextHandler {

  private final Route.Handler next;

  public RxSingleHandler(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
    try {
      Single result = (Single) next.apply(ctx);
      result.subscribe(ctx::render, x -> ctx.sendError((Throwable) x));
      return result;
    } catch (Throwable x) {
      ctx.sendError(x);
      return Single.error(x);
    }
  }

  @Override public Route.Handler next() {
    return next;
  }
}
