/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import static java.util.Optional.ofNullable;

import java.util.*;

import io.jooby.internal.openapi.ModelConvertersExt;
import io.swagger.v3.oas.models.media.Schema;

public class SchemaData {
  public static Map<String, Object> from(Class<?> schema) {
    return from(ModelConvertersExt.getInstance().read(schema).get(schema.getSimpleName()));
  }

  public static Map<String, Object> from(Schema<?> schema) {
    return ofNullable(traverse(schema.getProperties())).orElse(Map.of());
  }

  @SuppressWarnings("rawtypes")
  private static Map<String, Object> traverse(Map<String, Schema> properties) {
    if (properties != null) {
      Map<String, Object> result = new LinkedHashMap<>();
      properties.forEach(
          (name, value) -> {
            if (value.getType().equals("object")) {
              result.put(name, from(value));
            } else if (value.getType().equals("array")) {
              var array =
                  ofNullable(value.getItems())
                      .map(Schema::getProperties)
                      .map(SchemaData::traverse)
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
