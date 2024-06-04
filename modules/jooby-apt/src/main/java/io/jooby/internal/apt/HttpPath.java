/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import java.util.List;
import java.util.stream.Stream;

public enum HttpPath {
  PATH;
  private final List<String> annotations;

  private HttpPath() {
    this.annotations = List.of("io.jooby.annotation.Path", "jakarta.ws.rs.Path");
  }

  public List<String> getAnnotations() {
    return annotations;
  }

  public static boolean hasAnnotation(String name) {
    if (name == null) {
      return false;
    }
    return Stream.of(values()).anyMatch(it -> it.annotations.contains(name));
  }
}
