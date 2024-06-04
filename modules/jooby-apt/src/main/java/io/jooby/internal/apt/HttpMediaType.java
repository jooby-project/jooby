/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import java.util.List;
import java.util.stream.Stream;

public enum HttpMediaType {
  Consumes,
  Produces;
  private final List<String> annotations;

  private HttpMediaType(String... packages) {
    var packageList =
        packages.length == 0 ? List.of("io.jooby.annotation", "jakarta.ws.rs") : List.of(packages);
    this.annotations = packageList.stream().map(it -> it + "." + name()).toList();
  }

  public List<String> getAnnotations() {
    return annotations;
  }

  public static List<String> annotations() {
    return Stream.of(values()).flatMap(it -> it.annotations.stream()).toList();
  }
}
