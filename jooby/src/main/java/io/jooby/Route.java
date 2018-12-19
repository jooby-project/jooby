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
package io.jooby;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.List;

public interface Route {

  interface Decorator {
    @Nonnull Handler apply(@Nonnull Handler next);

    @Nonnull default Decorator then(@Nonnull Decorator next) {
      return h -> apply(next.apply(h));
    }

    @Nonnull default Handler then(@Nonnull Handler next) {
      return ctx -> apply(next).apply(ctx);
    }
  }

  interface Before extends Decorator {
    @Nonnull @Override default Handler apply(@Nonnull Handler next) {
      return ctx -> {
        before(ctx);
        return next.apply(ctx);
      };
    }

    void before(@Nonnull Context ctx) throws Exception;
  }

  interface After {

    @Nonnull default After then(@Nonnull After next) {
      return (ctx, result) -> apply(ctx, next.apply(ctx, result));
    }

    @Nonnull Object apply(@Nonnull Context ctx, Object result) throws Exception;
  }

  interface Handler extends Serializable {

    @Nonnull Object apply(@Nonnull Context ctx) throws Exception;

    @Nonnull default Object execute(@Nonnull Context ctx) {
      try {
        return apply(ctx);
      } catch (Throwable x) {
        ctx.sendError(x);
        return x;
      }
    }

    @Nonnull default Handler then(After next) {
      return ctx -> next.apply(ctx, apply(ctx));
    }
  }

  Handler NOT_FOUND = ctx -> ctx.sendError(new Err(StatusCode.NOT_FOUND));

  Handler METHOD_NOT_ALLOWED = ctx -> ctx.sendError(new Err(StatusCode.METHOD_NOT_ALLOWED));

  Handler FAVICON = ctx -> ctx.sendStatusCode(StatusCode.NOT_FOUND);

  @Nonnull String pattern();

  @Nonnull String method();

  @Nonnull List<String> pathKeys();

  @Nonnull Handler handler();

  @Nonnull Handler pipeline();

  @Nonnull Renderer renderer();

  @Nonnull Type returnType();
}
