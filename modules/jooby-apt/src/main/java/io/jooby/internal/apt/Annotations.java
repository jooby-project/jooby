/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;

/**
 * Annotation constants used by the APT.
 *
 * @since 2.1.0
 */
public interface Annotations {

  /**
   * Get an annotation value.
   *
   * @param mirror Annotation.
   * @param name Attribute name.
   * @return List of values.
   */
  static List<String> attribute(AnnotationMirror mirror, String name) {
    return attribute(mirror, name, v -> v.getValue().toString());
  }

  /**
   * Get an annotation value.
   *
   * @param mirror Annotation.
   * @param name Attribute name.
   * @param mapper Mapper function.
   * @param <T> Return type.
   * @return List of values.
   */
  static <T> List<T> attribute(
      AnnotationMirror mirror, String name, Function<AnnotationValue, T> mapper) {
    if (mirror != null) {
      for (var entry : mirror.getElementValues().entrySet()) {
        if (entry.getKey().getSimpleName().toString().equals(name)) {
          Object value = entry.getValue().getValue();
          if (value instanceof List) {
            List<AnnotationValue> values = (List<AnnotationValue>) value;
            return values.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
          }
          T singleValue = mapper.apply(entry.getValue());
          return singleValue == null
              ? Collections.emptyList()
              : Collections.singletonList(singleValue);
        }
      }
    }
    return Collections.emptyList();
  }
}
