/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

public enum HttpPath implements AnnotationSupport {
  PATH;
  private final List<String> annotations;

  private HttpPath() {
    this.annotations = List.of("io.jooby.annotation.Path", "jakarta.ws.rs.Path");
  }

  public List<String> getAnnotations() {
    return annotations;
  }

  public List<String> path(List<TypeElement> hierarchy) {
    var prefix = Collections.<String>emptyList();
    // Look at parent @path annotation
    var i = 0;
    while (prefix.isEmpty() && i < hierarchy.size()) {
      prefix = path(hierarchy.get(i++));
    }
    return prefix;
  }

  public List<String> path(Element element) {
    return getAnnotations().stream()
        .map(it -> AnnotationSupport.findAnnotationByName(element, it))
        .filter(Objects::nonNull)
        .findFirst()
        .map(it -> AnnotationSupport.findAnnotationValue(it, VALUE))
        .orElseGet(List::of);
  }

  public static boolean hasAnnotation(String name) {
    if (name == null) {
      return false;
    }
    return Stream.of(values()).anyMatch(it -> it.annotations.contains(name));
  }
}
