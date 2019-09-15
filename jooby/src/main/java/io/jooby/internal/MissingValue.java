/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.MissingValueException;
import io.jooby.ValueNode;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MissingValue implements ValueNode {
  private String name;

  public MissingValue(String name) {
    this.name = name;
  }

  @Override public String name() {
    return name;
  }

  @Override public ValueNode get(@Nonnull String name) {
    return this.name.equals(name) ? this : new MissingValue(this.name + "." + name);
  }

  @Override public ValueNode get(@Nonnull int index) {
    return new MissingValue(this.name + "[" + index + "]");
  }

  @Nonnull @Override public <T> T to(@Nonnull Class<T> type) {
    return null;
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
