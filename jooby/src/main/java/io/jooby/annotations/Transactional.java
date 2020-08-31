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
 * Useful together with the various route decorators like {@code TransactionalRequest}
 * provided by extensions {@code jooby-hibernate}, {@code jooby-jdbi} or {@code jooby-ebean}
 * to toggle it's effect for a single route.
 * <p>
 * Although {@code TransactionalRequest} is configured to be enabled by default,
 * for a route method annotated with {@code @Transactional(false)} it won't take effect.
 * <p>
 * Similarly, if the decorator is disabled by default, a for route method annotated with
 * {@code @Transactional(true)} it will take effect.
 * <p>
 * Use the {@link #ATTRIBUTE} constant for script routes instead of the annotation itself:
 * <pre>{@code
 * {
 *   get("/", ctx -> ...).attribute(Transactional.ATTRIBUTE, true);
 * }
 * }</pre>
 * <p>
 * This annotation has no effect on the behavior of {@code SessionRequest} decorator(s).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Transactional {

  /**
   * Whether to enable or disable {@code TransactionalRequest} for the annotated route.
   *
   * @return value to toggle {@code TransactionalRequest}
   */
  boolean value() default true;

  /**
   * Constant to use as attribute name for script routes.
   *
   * <pre>{@code
   * {
   *   get("/", ctx -> ...).attribute(Transactional.ATTRIBUTE, true);
   * }
   * }</pre>
   */
  String ATTRIBUTE = Transactional.class.getSimpleName();
}
