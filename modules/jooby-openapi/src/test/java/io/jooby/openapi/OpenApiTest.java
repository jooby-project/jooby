package io.jooby.openapi;

import io.jooby.internal.openapi.DebugOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(METHOD)
@Retention(RUNTIME)
@Test
@ExtendWith(OpenApiExtension.class)
public @interface OpenApiTest {
  Class value();

  DebugOption[] debug() default {};

  boolean ignoreArguments() default false;
}
