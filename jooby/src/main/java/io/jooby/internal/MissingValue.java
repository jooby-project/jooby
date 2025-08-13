/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.util.*;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.exception.MissingValueException;
import io.jooby.value.Value;
import io.jooby.value.ValueFactory;

public class MissingValue implements Value {
  private final ValueFactory factory;
  private final String name;

  public MissingValue(ValueFactory factory, String name) {
    this.factory = factory;
    this.name = name;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public @NonNull Value get(@NonNull String name) {
    return this.name.equals(name) ? this : new MissingValue(factory, this.name + "." + name);
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public @NonNull Iterator<Value> iterator() {
    return Collections.emptyIterator();
  }

  @Override
  public @NonNull Value get(int index) {
    return new MissingValue(factory, this.name + "[" + index + "]");
  }

  @Override
  public Value getOrDefault(@NonNull String name, @NonNull String defaultValue) {
    return Value.value(factory, name, defaultValue);
  }

  @NonNull @Override
  public <T> T to(@NonNull Class<T> type) {
    throw new MissingValueException(name);
  }

  @Nullable @Override
  public <T> T toNullable(@NonNull Class<T> type) {
    return null;
  }

  @Override
  public String value() {
    throw new MissingValueException(name);
  }

  @NonNull @Override
  public Map<String, String> toMap() {
    return Collections.emptyMap();
  }

  @Override
  public Map<String, List<String>> toMultimap() {
    return Collections.emptyMap();
  }

  @NonNull @Override
  public List<String> toList() {
    return Collections.emptyList();
  }

  @NonNull @Override
  public Optional<String> toOptional() {
    return Optional.empty();
  }

  @NonNull @Override
  public <T> List<T> toList(@NonNull Class<T> type) {
    return Collections.emptyList();
  }

  @NonNull @Override
  public Set<String> toSet() {
    return Collections.emptySet();
  }

  @NonNull @Override
  public <T> Set<T> toSet(@NonNull Class<T> type) {
    return Collections.emptySet();
  }

  @Override
  public String toString() {
    return "<missing>";
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof MissingValue) {
      return Objects.equals(name, ((MissingValue) o).name);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }
}
