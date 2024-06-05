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

/**
 * Consumes/Produces annotation support.
 *
 * @author edgar
 */
public enum HttpMediaType {
  Consumes,
  Produces;
  private final List<String> annotations;

  HttpMediaType() {
    this.annotations =
        Stream.of("io.jooby.annotation", "jakarta.ws.rs").map(it -> it + "." + name()).toList();
  }

  /**
   * Get value from Consumes/Produces annotation.
   *
   * @param element Method or Class.
   * @return Media type values or empty list.
   */
  public List<String> mediaType(Element element) {
    return annotations.stream()
        .map(it -> findAnnotationByName(element, it))
        .filter(Objects::nonNull)
        .findFirst()
        .map(it -> findAnnotationValue(it, VALUE))
        .orElseGet(List::of);
  }
}
