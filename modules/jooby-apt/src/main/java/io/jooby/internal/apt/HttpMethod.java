/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import java.util.List;
import java.util.stream.Stream;

import javax.lang.model.element.TypeElement;

public enum HttpMethod {
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

  public static boolean hasAnnotation(TypeElement element) {
    if (element == null) {
      return false;
    }
    var names = Stream.of(element.toString(), element.asType().toString()).distinct().toList();
    return Stream.of(values()).anyMatch(it -> it.annotations.stream().anyMatch(names::contains));
  }

  public static List<String> annotations() {
    return Stream.of(values()).flatMap(it -> it.annotations.stream()).toList();
  }
}
