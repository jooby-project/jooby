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
 * Marks a class or its methods as a JSON-RPC 2.0 endpoint.
 *
 * <h3>Discovery Rules:</h3>
 *
 * <ul>
 *   <li><b>Implicit Mapping:</b> If a class is annotated with {@code @JsonRpc} and <b>no</b>
 *       methods are explicitly annotated, <b>all</b> public methods are automatically exposed as
 *       JSON-RPC endpoints.
 *   <li><b>Explicit Mapping:</b> If at least <b>one</b> method in the class is explicitly annotated
 *       with {@code @JsonRpc}, implicit mapping is disabled. <b>Only</b> the annotated methods will
 *       be exposed, requiring you to map any other desired endpoints one by one.
 * </ul>
 *
 * <h3>Naming & Routing Rules:</h3>
 *
 * <ul>
 *   <li><b>Class Level (Namespace):</b> When applied to a class, the {@link #value()} defines the
 *       namespace for all JSON-RPC methods within that class. If the annotation is present but the
 *       value is empty (the default), <b>no namespace</b> is applied.
 *   <li><b>Method Level (Method Name):</b> When applied to a method, the {@link #value()} defines
 *       the exact RPC method name. If the value is empty, the actual Java/Kotlin method name is
 *       used.
 * </ul>
 *
 * <p>The final JSON-RPC method string expected by the dispatcher is formatted as {@code
 * "namespace.methodName"} (or just {@code "methodName"} if no namespace exists).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonRpc {

  /**
   * The explicit namespace (when used on a class) or the explicit method name (when used on a
   * method). * @return The overridden name, or an empty string to use the defaults (no namespace
   * for classes, actual method name for methods).
   */
  String value() default "";
}
