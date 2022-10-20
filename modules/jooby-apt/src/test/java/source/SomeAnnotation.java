/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package source;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SomeAnnotation {
  String value();

  double d();

  float f();

  int i();

  long l();

  char c();

  short s();

  Class type();

  boolean bool() default true;

  String[] values() default {};

  /** Must be ignored. */
  LinkAnnotation annotation();
}
