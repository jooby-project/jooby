/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

public enum HttpPath implements AnnotationSupport {
  PATH;
  private final List<String> annotations;

  HttpPath() {
    this.annotations = List.of("io.jooby.annotation.Path", "jakarta.ws.rs.Path");
  }

  public List<String> getAnnotations() {
    return annotations;
  }

  /**
   * Find path on type hierarchy. It goes back at hierarchy until it finds a Path annotation.
   *
   * @param hierarchy Type hierarchy.
   * @return Path or empty list.
   */
  public List<String> path(Collection<TypeElement> hierarchy) {
    return path(hierarchy, getAnnotations());
  }

  private List<String> path(Collection<TypeElement> hierarchy, List<String> annotations) {
    var prefix = Collections.<String>emptyList();
    var it = hierarchy.iterator();
    while (prefix.isEmpty() && it.hasNext()) {
      prefix = path(it.next(), annotations);
    }
    return prefix;
  }

  /**
   * Find Path from method or class.
   *
   * @param element Method or Class.
   * @return Path or empty list.
   */
  public List<String> path(Element element) {
    return path(element, getAnnotations());
  }

  /**
   * Find Path from method or class.
   *
   * @param element Method or Class.
   * @return Path or empty list.
   */
  private List<String> path(Element element, List<String> annotations) {
    return annotations.stream()
        .map(it -> AnnotationSupport.findAnnotationByName(element, it))
        .filter(Objects::nonNull)
        .findFirst()
        .map(it -> AnnotationSupport.findAnnotationValue(it, VALUE))
        .orElseGet(List::of);
  }
}
