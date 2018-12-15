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

public interface Renderer {

  Renderer TO_STRING = (ctx, value) -> ctx.sendText(value.toString());

  void render(@Nonnull Context ctx, @Nonnull Object result) throws Exception;

  @Nonnull default Renderer then(@Nonnull Renderer next) {
    return (ctx, result) -> {
      render(ctx, result);
      if (!ctx.isResponseStarted()) {
        next.render(ctx, result);
      }
    };
  }

  @Nonnull default Renderer matches(String contentType) {
    return (ctx, result) -> {
      if (ctx.accept(contentType)) {
        render(ctx, result);
      }
    };
  }

}
