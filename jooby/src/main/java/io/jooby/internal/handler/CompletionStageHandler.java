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

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class CompletionStageHandler implements Route.Handler {

  private final Route.Handler next;

  public CompletionStageHandler(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
    try {
      CompletionStage result = (CompletionStage) next.apply(ctx);
      return result.whenComplete((value, x) -> {
        try {
          if (x != null) {
            ctx.sendError((Throwable) x);
          } else {
            ctx.render(value);
          }
        } catch (Throwable newx) {
          ctx.sendError(newx);
        }
      });
    } catch (Throwable x) {
      ctx.sendError(x);
      return CompletableFuture.completedFuture(x);
    }
  }

  @Override public Route.Handler next() {
    return next;
  }
}
