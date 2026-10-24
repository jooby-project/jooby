/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Server-sent event route for MVC controllers. The method must return {@code void} and may receive
 * a {@link io.jooby.ServerSentEmitter} alongside regular MVC parameters.
 *
 * <pre>{@code
 * @SSE("/sse")
 * public void sse(ServerSentEmitter emitter) {
 *   emitter.send("Welcome");
 *   emitter.onClose(() -> {});
 *   emitter.close();
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SSE {
  /**
   * @return Path patterns. Shortcut for {@link #path()}.
   */
  String[] value() default {};

  /**
   * @return Path patterns.
   */
  String[] path() default {};
}
