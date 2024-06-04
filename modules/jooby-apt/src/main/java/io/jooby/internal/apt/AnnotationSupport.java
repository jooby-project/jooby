/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;

public interface AnnotationSupport {
  Predicate<String> VALUE = "value"::equals;

  static List<String> findAnnotationValue(
      AnnotationMirror annotation, Predicate<String> predicate) {
    return annotation == null
        ? List.of()
        : annotation.getElementValues().entrySet().stream()
            .filter(it -> predicate.test(it.getKey().getSimpleName().toString()))
            .flatMap(
                it ->
                    Stream.of(it.getValue().getValue())
                        .filter(Objects::nonNull)
                        .flatMap(value -> annotationValue(value).stream())
                        .map(Objects::toString))
            .toList();
  }

  static List<Object> annotationValue(Object value) {
    if (value instanceof VariableElement variable) {
      return List.of(variable.getSimpleName().toString());
    } else if (value instanceof AnnotationValue) {
      return List.of(((AnnotationValue) value).getValue());
    } else if (value instanceof List values) {
      return values.stream().flatMap(it -> annotationValue(it).stream()).toList();
    } else {
      return List.of(value);
    }
  }

  static AnnotationMirror findAnnotationByName(Element element, String annotationName) {
    return element.getAnnotationMirrors().stream()
        .filter(it -> it.getAnnotationType().asElement().toString().equals(annotationName))
        .findFirst()
        .orElse(null);
  }
}
