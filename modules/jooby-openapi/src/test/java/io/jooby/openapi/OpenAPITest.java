/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.openapi;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@Target(METHOD)
@Retention(RUNTIME)
@Test
@ExtendWith(OpenAPIExtension.class)
public @interface OpenAPITest {
  Class value();

  DebugOption[] debug() default {};

  boolean ignoreArguments() default false;

  String includes() default "";

  String excludes() default "";

  String templateName() default "";
}
