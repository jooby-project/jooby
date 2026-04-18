/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import io.jooby.value.ConversionHint;
import io.jooby.value.Value;
import io.jooby.value.ValueFactory;

public class SingleValue implements Value {

  private final ValueFactory factory;

  private final String name;

  private final String value;

  public SingleValue(ValueFactory factory, String name, String value) {
    this.factory = factory;
    this.name = name;
    this.value = value;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Value get(int index) {
    return get(Integer.toString(index));
  }

  @Override
  public Value get(String name) {
    return new MissingValue(factory, this.name + "." + name);
  }

  @Override
  public Value getOrDefault(String name, String defaultValue) {
    return Value.value(factory, this.name + "." + name, defaultValue);
  }

  @Override
  public int size() {
    return 1;
  }

  @Override
  public String value() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }

  @Override
  public Iterator<Value> iterator() {
    return List.of((Value) this).iterator();
  }

  @Override
  public <T> List<T> toList(Class<T> type) {
    return Collections.singletonList(to(type));
  }

  @Override
  public <T> Set<T> toSet(Class<T> type) {
    return Collections.singleton(to(type));
  }

  @Override
  public <T> Optional<T> toOptional(Class<T> type) {
    return Optional.ofNullable(toNullable(type));
  }

  @Override
  public <T> T to(Class<T> type) {
    return factory.convert(type, this);
  }

  @Nullable @Override
  public <T> T toNullable(Class<T> type) {
    return factory.convert(type, this, ConversionHint.Nullable);
  }

  @Override
  public Map<String, List<String>> toMultimap() {
    return Map.of(name, List.of(value));
  }

  @Override
  public List<String> toList() {
    return List.of(value);
  }

  @Override
  public Set<String> toSet() {
    return Set.of(value);
  }
}
