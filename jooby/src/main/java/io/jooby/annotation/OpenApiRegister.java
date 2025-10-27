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
 * Allows manually specifying MVC routes to be included in openapi documentation generation.
 *
 * <pre>{@code
 * @OpenApiRegister({Controller1.class, Controller2.class})
 * public class App extends Jooby {
 *   ...
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface OpenApiRegister {

  /**
   * List of MVC controller class names to register for openapi generation
   *
   * @return MVC controller classes.
   */
  Class<?>[] value();
}
