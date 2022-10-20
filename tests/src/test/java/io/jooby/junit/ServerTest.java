/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.junit;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import io.jooby.ExecutionMode;

@Target({TYPE, METHOD})
@Retention(RUNTIME)
@TestTemplate
@ExtendWith(ServerExtensionImpl.class)
public @interface ServerTest {
  Class[] server() default {};

  ExecutionMode[] executionMode() default ExecutionMode.DEFAULT;

  int iterations() default 1;
}
