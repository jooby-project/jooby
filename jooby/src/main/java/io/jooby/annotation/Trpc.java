/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks a controller class or a specific route method for tRPC TypeScript generation. */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Trpc {

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @interface Mutation {
    /**
     * Custom name for the tRPC procedure.
     *
     * <p>If applied to a method, this overrides the generated procedure name. If applied to a
     * class, this overrides the generated namespace/router name.
     *
     * @return The custom procedure name. Empty by default, which means the generator will use the
     *     Java method or class name.
     */
    String value() default "";
  }

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @interface Query {
    /**
     * Custom name for the tRPC procedure.
     *
     * <p>If applied to a method, this overrides the generated procedure name. If applied to a
     * class, this overrides the generated namespace/router name.
     *
     * @return The custom procedure name. Empty by default, which means the generator will use the
     *     Java method or class name.
     */
    String value() default "";
  }

  /**
   * Custom name for the tRPC procedure or namespace.
   *
   * <p>If applied to a method, this overrides the generated procedure name. If applied to a class,
   * this overrides the generated namespace/router name.
   *
   * @return The custom procedure or namespace name. Empty by default, which means the generator
   *     will use the Java method or class name.
   */
  String value() default "";
}
