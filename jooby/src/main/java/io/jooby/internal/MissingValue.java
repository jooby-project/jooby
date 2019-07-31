/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.MissingValueException;
import io.jooby.Value;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MissingValue implements Value {
  private String name;

  public MissingValue(String name) {
    this.name = name;
  }

  @Override public String name() {
    return name;
  }

  @Override public Value get(@Nonnull String name) {
    return this.name.equals(name) ? this : new MissingValue(this.name + "." + name);
  }

  @Override public Value get(@Nonnull int index) {
    return new MissingValue(this.name + "[" + index + "]");
  }

  @Override public String value() {
    throw new MissingValueException(name);
  }

  @Override public Map<String, List<String>> toMultimap() {
    return Collections.emptyMap();
  }

  @Override public String toString() {
    return "<missing>";
  }
}
