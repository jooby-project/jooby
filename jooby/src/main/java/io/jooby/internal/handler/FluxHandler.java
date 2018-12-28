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
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;

public class FluxHandler implements ChainedHandler {

  private final Route.Handler next;

  public FluxHandler(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
    try {
      Flux result = (Flux) next.apply(ctx);
      result.subscribe(new ContextSubscriber(ctx));
      return result;
    } catch (Throwable x) {
      ctx.sendError(x);
      return Flux.error(x);
    }
  }

  @Override public Route.Handler next() {
    return next;
  }
}
