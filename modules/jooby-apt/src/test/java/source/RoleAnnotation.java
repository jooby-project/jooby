package source;

public @interface RoleAnnotation {
  String value();

  String level() default "one";
}
