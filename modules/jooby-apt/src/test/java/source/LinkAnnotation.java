package source;

public @interface LinkAnnotation {
  String value() default "";

  ArrayAnnotation[] array() default {};
}
