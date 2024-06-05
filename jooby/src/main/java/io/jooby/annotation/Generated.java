/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({
  ElementType.PACKAGE,
  ElementType.TYPE,
  ElementType.ANNOTATION_TYPE,
  ElementType.METHOD,
  ElementType.CONSTRUCTOR,
  ElementType.FIELD,
  ElementType.LOCAL_VARIABLE,
  ElementType.PARAMETER
})
public @interface Generated {
  String value();
}
