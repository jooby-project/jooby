package source;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface LinkAnnotation {
  String value() default "";

  ArrayAnnotation[] array() default {};
}
