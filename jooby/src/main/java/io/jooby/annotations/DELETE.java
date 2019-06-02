/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * HTTP DELETE verb for mvc routes.
 * <pre>
 *   class Resources {
 *
 *     &#64;DELETE
 *     public void method() {
 *     }
 *   }
 * </pre>
 *
 * @author edgar
 * @since 0.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DELETE {
  /**
   * Path pattern. This is a shortcut for {@link #path()}.
   *
   * @return Path pattern.
   */
  String[] value() default {};

  /**
   * Path pattern.
   *
   * @return Path pattern.
   */
  String[] path() default {};

  /**
   * Produce types. Check the <code>Accept</code> header against this value or send a
   * "406 Not Acceptable" response.
   *
   * @return Produce types.
   */
  String[] produces() default {};

  /**
   * Consume types. Check the <code>Content-Type</code> header against this value or send a
   * "415 Unsupported Media Type" response.
   *
   * @return Consume types.
   */
  String[] consumes() default {};
}
