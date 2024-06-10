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
 * Hints source code generator (jooby annotation processor) to map/adapt a specific return type to
 * use a custom handler. This annotation if only for source code generator process so only applies
 * for MVC routes. Example:
 *
 * <pre>{@code
 * class MyController {
 *   @GET("/")
 *   public MySpecialType hello() {}
 * }
 * }</pre>
 *
 * Write a code generator:
 *
 * <pre>{@code
 * @ResultType(types = MySpecialType.class, handler = "customMapping")
 * class MySpecialTypeGenerator {
 *
 *     public static Route.Handler customMapping(Route.Handler handler) {
 *         return myHandler.then(handler);
 *     }
 * }
 * }</pre>
 *
 * Let jooby annotation processor to know about your handler by setting the <code>jooby.handler
 * </code> annotation processor option: <code>jooby.handler=mypackage.MySpecialTypeGenerator</code>
 * Generates:
 *
 * <pre>{@code
 * app.get("/", customMapping(this::hello));
 * }</pre>
 *
 * @author edgar
 * @since 3.2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ResultType {
  /**
   * Custom type that requires special handling.
   *
   * @return Types.
   */
  Class<?>[] types();

  /**
   * Mapping function must be: - Single argument function of type {@link io.jooby.Route.Handler}. -
   * Returns type {@link io.jooby.Route.Handler}. - Must be static.
   *
   * @return Name of mapping function.
   */
  String handler();
}
