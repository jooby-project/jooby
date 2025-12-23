/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import java.util.Optional;

import org.objectweb.asm.tree.AnnotationNode;

public final class AnnotationUtils {

  public static Optional<Object> findAnnotationValue(AnnotationNode node, String name) {
    for (int i = 0; i < node.values.size(); i++) {
      if (node.values.get(i).equals(name)) {
        Object value = node.values.get(i + 1);
        if (value != null && !value.toString().trim().isEmpty()) {
          return value instanceof String
              ? Optional.of(value.toString().trim())
              : Optional.of(value);
        }
      }
    }
    return Optional.empty();
  }
}
