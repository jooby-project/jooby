/*
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

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.Value;
import io.jooby.exception.MissingValueException;

public class MissingValue implements Value {
  private String name;

  public MissingValue(String name) {
    this.name = name;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public @NonNull Value get(@NonNull String name) {
    return this.name.equals(name) ? this : new MissingValue(this.name + "." + name);
  }

  @Override
  public @NonNull Value get(int index) {
    return new MissingValue(this.name + "[" + index + "]");
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
