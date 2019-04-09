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
package io.jooby;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;

/**
 * Bind a route response type to a custom handler. The response handler works like a map function,
 * which applies to a specific response type.
 *
 * For example if your route produces a <code>Foo</code> type as response. You can write a
 * FooHandler that knows how to render the <code>Foo</code> object.
 *
 * Mapping is done efficiently, it doesn't test every single route response at runtime. Instead
 * analysis is done only once at application startup time, it generates a unique route pipeline
 * for all the routes that generates a <code>Foo</code> output.
 *
 * Example:
 *
 * <pre>{@code
 * {
 *   boolean matches(Type type) {
 *     return Foo.class == type;
 *   }
 *
 *   Route.Handler create(Route.Handler next) {
 *     return ctx -> {
 *       Foo foo = (Foo) next.apply(ctx);
 *       return ctx.sendString(foo.toString());
 *     }
 *   }
 * }
 * }</pre>
 *
 * @since 2.0.0
 * @author edgar
 */
public interface ResponseHandler {

  /**
   * True if response route type is the one expected by the response handler.
   *
   * @param type Type to test.
   * @return True if response route type is the one expected by the response handler.
   */
  boolean matches(@Nonnull Type type);

  /**
   * Creates a handler for a response type. Example:
   *
   * <pre>{@code
   * {
   *   boolean matches(Type type) {
   *     return Foo.class == type;
   *   }
   *
   *   Route.Handler create(Route.Handler next) {
   *     return ctx -> {
   *       Foo foo = (Foo) next.apply(ctx);
   *       return ctx.sendString(foo.toString());
   *     }
   *   }
   * }
   * }</pre>
   *
   * @param next Next route in pipeline (usually the route handler).
   * @return A response handler.
   */
  @Nonnull Route.Handler create(Route.Handler next);
}
