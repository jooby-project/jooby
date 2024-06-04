/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import static io.jooby.internal.apt.AnnotationSupport.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javax.lang.model.element.Element;

public enum HttpMediaType {
  Consumes,
  Produces;
  private final List<String> annotations;

  HttpMediaType(String... packages) {
    var packageList =
        packages.length == 0 ? List.of("io.jooby.annotation", "jakarta.ws.rs") : List.of(packages);
    this.annotations = packageList.stream().map(it -> it + "." + name()).toList();
  }

  public List<String> mediaType(Element element) {
    return getAnnotations().stream()
        .map(it -> findAnnotationByName(element, it))
        .filter(Objects::nonNull)
        .findFirst()
        .map(it -> findAnnotationValue(it, VALUE))
        .orElseGet(List::of);
  }

  public List<String> getAnnotations() {
    return annotations;
  }

  public static List<String> annotations() {
    return Stream.of(values()).flatMap(it -> it.annotations.stream()).toList();
  }
}
