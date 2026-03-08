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
 * Marks a class and its methods as a JSON-RPC 2.0 endpoint. *
 *
 * <p>To expose a method via JSON-RPC, the method <b>must</b> be annotated with {@code @JsonRpc}. If
 * a class contains methods annotated with {@code @JsonRpc}, the class itself can optionally be
 * annotated to define a common namespace. *
 *
 * <h3>Routing Rules:</h3>
 *
 * <ul>
 *   <li><b>Class Level (Namespace):</b> When applied to a class, the {@link #value()} defines the
 *       namespace for all JSON-RPC methods within that class. If the annotation is present but the
 *       value is empty, the simple class name (e.g., {@code MovieService}) is used as the
 *       namespace. If the class is not annotated at all, the methods are registered without a
 *       namespace. *
 *   <li><b>Method Level (Method Name):</b> When applied to a method, the method is exposed over
 *       JSON-RPC. The {@link #value()} defines the exact RPC method name. If the value is empty,
 *       the actual Java/Kotlin method name is used.
 * </ul>
 *
 * *
 *
 * <p>The final JSON-RPC method string expected by the dispatcher is formatted as {@code
 * "namespace.methodName"} (or just {@code "methodName"} if no namespace exists).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonRpc {

  /**
   * The explicit namespace (when used on a class) or the explicit method name (when used on a
   * method). * @return The overridden name, or an empty string to use the defaults (Simple Class
   * Name or Method Name).
   */
  String value() default "";
}
