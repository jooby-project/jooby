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
