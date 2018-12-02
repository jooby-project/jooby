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
package io.jooby.internal;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.StatusCode;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.function.Function;

public class RootErrorHandlerImpl implements Route.RootErrorHandler {
  private final Route.ErrorHandler next;
  private final Function<Throwable, StatusCode> statusCode;
  private final Logger log;

  public RootErrorHandlerImpl(Route.ErrorHandler next, Logger log, Function<Throwable, StatusCode> statusCode) {
    this.next = next;
    this.log = log;
    this.statusCode = statusCode;
  }

  @Override public void apply(@Nonnull Context ctx, @Nonnull Throwable cause) {
    if (ctx.isResponseStarted()) {
     log.error("execution resulted in exception and response was already sent", cause);
    } else {
      next.apply(ctx, cause, statusCode.apply(cause));
    }
  }
}
