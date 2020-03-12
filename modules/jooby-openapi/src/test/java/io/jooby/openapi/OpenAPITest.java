package io.jooby.openapi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

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
}
