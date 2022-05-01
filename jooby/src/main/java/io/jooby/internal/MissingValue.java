/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import io.jooby.ValueNode;
import io.jooby.exception.MissingValueException;

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

  @Nonnull @Override public Map<String, String> toMap() {
    return Collections.emptyMap();
  }

  @Override public Map<String, List<String>> toMultimap() {
    return Collections.emptyMap();
  }

  @Nonnull @Override public List<String> toList() {
    return Collections.emptyList();
  }

  @Nonnull @Override public Optional<String> toOptional() {
    return Optional.empty();
  }

  @Nonnull @Override public <T> List<T> toList(@Nonnull Class<T> type) {
    return Collections.emptyList();
  }

  @Nonnull @Override public Set<String> toSet() {
    return Collections.emptySet();
  }

  @Nonnull @Override public <T> Set<T> toSet(@Nonnull Class<T> type) {
    return Collections.emptySet();
  }

  @Override public String toString() {
    return "<missing>";
  }

  @Override public boolean equals(Object o) {
    if (o instanceof MissingValue) {
      return Objects.equals(name, ((MissingValue) o).name);
    }
    return false;
  }

  @Override public int hashCode() {
    return Objects.hash(name);
  }
}
