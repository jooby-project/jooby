/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import io.swagger.v3.oas.models.media.Schema;

public record HttpParam(
    String name, Schema<?> schema, Object value, String in, String description) {

  public String get(String field) {
    return switch (field) {
      case "name" -> name;
      case "value" -> value == null ? "" : value.toString();
      case "type" -> Optional.ofNullable(schema.getFormat()).orElse(schema.getType());
      case "in" -> in == null ? "" : in;
      default ->
          Stream.of(description, schema.getDescription())
              .filter(Objects::nonNull)
              .findFirst()
              .orElse("");
    };
  }
}
