/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import static java.util.Optional.ofNullable;

import java.util.*;
import java.util.function.Function;

import io.swagger.v3.oas.models.media.Schema;

public class SchemaData {
  public static Map<String, Object> from(
      Schema<?> schema, Function<String, Optional<Schema<?>>> resolver) {
    return ofNullable(traverse(schema.getProperties(), resolver)).orElse(Map.of());
  }

  @SuppressWarnings("rawtypes")
  private static Map<String, Object> traverse(
      Map<String, Schema> properties, Function<String, Optional<Schema<?>>> resolver) {
    if (properties != null) {
      Map<String, Object> result = new LinkedHashMap<>();
      properties.forEach(
          (name, value) -> {
            if (value.getType() == null) {
              // must be a reference
              var ref = value.get$ref();
              if (ref != null) {
                var refSchema = resolver.apply(ref);
                if (refSchema.isPresent()) {
                  result.put(name, from(refSchema.get(), resolver));
                } else {
                  // resolve as empty/missing
                  result.put(name, Map.of());
                }
              } else {
                // resolve as empty/missing
                result.put(name, Map.of());
              }
            } else if (value.getType().equals("object")) {
              result.put(name, from(value, resolver));
            } else if (value.getType().equals("array")) {
              var array =
                  ofNullable(value.getItems())
                      .map(Schema::getProperties)
                      .map(it -> traverse(it, resolver))
                      .map(List::of)
                      .orElse(List.of());
              result.put(name, array);
            } else {
              result.put(name, shemaType(value));
            }
          });
      return result;
    }
    return null;
  }

  public static String shemaType(Schema<?> schema) {
    return Optional.ofNullable(schema.getFormat()).orElse(schema.getType());
  }
}
