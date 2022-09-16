package io.jooby.junit;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import io.jooby.ExecutionMode;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({TYPE, METHOD})
@Retention(RUNTIME)
@TestTemplate
@ExtendWith(ServerExtensionImpl.class)
public @interface ServerTest {
  Class[] server() default {};

  ExecutionMode[] executionMode () default ExecutionMode.DEFAULT;

  int iterations() default 1;
}
