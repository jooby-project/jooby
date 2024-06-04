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

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

public enum HttpMethod implements AnnotationSupport {
  CONNECT("io.jooby.annotation"),
  TRACE("io.jooby.annotation"),
  DELETE,
  GET,
  HEAD,
  OPTIONS,
  PATCH,
  POST,
  PUT;
  private final List<String> annotations;

  private HttpMethod(String... packages) {
    var packageList =
        packages.length == 0 ? List.of("io.jooby.annotation", "jakarta.ws.rs") : List.of(packages);
    this.annotations = packageList.stream().map(it -> it + "." + name()).toList();
  }

  public List<String> getAnnotations() {
    return annotations;
  }

  public List<String> path(Element element) {
    var path =
        getAnnotations().stream()
            .map(it -> AnnotationSupport.findAnnotationByName(element, it))
            .filter(Objects::nonNull)
            .findFirst()
            .map(it -> AnnotationSupport.findAnnotationValue(it, VALUE.or("path"::equals)))
            .orElseGet(List::of);
    return path.isEmpty() ? HttpPath.PATH.path(element) : path;
  }

  public List<String> consumes(Element element) {
    return mediaType(element, HttpMediaType.Consumes, "consumes"::equals);
  }

  public List<String> produces(Element element) {
    return mediaType(element, HttpMediaType.Produces, "produces"::equals);
  }

  private List<String> mediaType(
      Element element, HttpMediaType mediaType, Predicate<String> filter) {
    var path =
        getAnnotations().stream()
            .map(it -> AnnotationSupport.findAnnotationByName(element, it))
            .filter(Objects::nonNull)
            .findFirst()
            .map(it -> AnnotationSupport.findAnnotationValue(it, filter))
            .orElseGet(List::of);
    return Stream.concat(path.stream(), mediaType.mediaType(element).stream()).toList();
  }

  public static boolean hasAnnotation(TypeElement element) {
    if (element == null) {
      return false;
    }
    var names = Stream.of(element.toString(), element.asType().toString()).distinct().toList();
    return Stream.of(values()).anyMatch(it -> it.annotations.stream().anyMatch(names::contains));
  }

  public static HttpMethod findByAnnotationName(String name) {
    return Stream.of(values())
        .filter(it -> it.annotations.contains(name))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Not found: " + name));
  }

  public static List<String> annotations() {
    return Stream.of(values()).flatMap(it -> it.annotations.stream()).toList();
  }
}
