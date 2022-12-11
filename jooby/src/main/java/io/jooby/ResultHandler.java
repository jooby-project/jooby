/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.lang.reflect.Type;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Bind a route response type to a custom handler. The response handler works like a map function,
 * which applies to a specific response type.
 *
 * <p>For example if your route produces a <code>Foo</code> type as response. You can write a
 * FooHandler that knows how to render the <code>Foo</code> object.
 *
 * <p>Mapping is done efficiently, it doesn't test every single route response at runtime. Instead
 * analysis is done only once at application startup time, it generates a unique route pipeline for
 * all the routes that generates a <code>Foo</code> output.
 *
 * <p>Example:
 *
 * <pre>{@code
 * {
 *   boolean matches(Type type) {
 *     return Foo.class == type;
 *   }
 *
 *   Route.Filter create() {
 *     return next -> ctx -> {
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
public interface ResultHandler {

  /**
   * True if response route type is the one expected by the response handler.
   *
   * @param type Type to test.
   * @return True if response route type is the one expected by the response handler.
   */
  boolean matches(@NonNull Type type);

  boolean isReactive();

  /**
   * Creates a handler for a response type. Example:
   *
   * <pre>{@code
   * {
   *   boolean matches(Type type) {
   *     return Foo.class == type;
   *   }
   *
   *   Route.Filter create() {
   *     return next -> ctx -> {
   *       Foo foo = (Foo) next.apply(ctx);
   *       return ctx.sendString(foo.toString());
   *     }
   *   }
   * }
   * }</pre>
   *
   * @return A response handler.
   */
  @NonNull Route.Filter create();
}
