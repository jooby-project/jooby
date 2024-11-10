/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import static java.util.Optional.ofNullable;

import java.util.Map;
import java.util.Objects;

public enum MergePolicy {
  FAIL {
    @Override
    public boolean handle(String message) {
      throw new IllegalArgumentException(message);
    }
  },
  KEEP {
    @Override
    public boolean handle(String message) {
      return true;
    }
  },
  IGNORE {
    @Override
    public boolean handle(String message) {
      return false;
    }
  };

  public static MergePolicy parse(Map<String, Object> extensions, MergePolicy defaultPolicy) {
    if (extensions == null) {
      return defaultPolicy;
    }
    var value = extensions.remove("x-merge-policy");
    return ofNullable(value)
        .map(Objects::toString)
        .map(String::toUpperCase)
        .map(MergePolicy::valueOf)
        .orElse(defaultPolicy);
  }

  public abstract boolean handle(String message);
}
