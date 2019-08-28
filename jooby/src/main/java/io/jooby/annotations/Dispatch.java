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
 * Dispatch operator for MVC routes.
 *
 * <pre>
 *   &#64;Path("/r")
 *   class Resources {
 *
 *     &#64;Dispatch
 *     &#64;GET
 *     public ... dispatch() {
 *       // do blocking calls
 *     }
 *   }
 * </pre>
 *
 * @author edgar
 * @since 2.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD })
public @interface Dispatch {
  /**
   * Name of the executor to use or blank to use the server worker executor.
   *
   * @return Name of the executor to use or blank to use the server worker executor.
   */
  String value() default "worker";
}
