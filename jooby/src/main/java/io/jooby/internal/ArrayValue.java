/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.exception.MissingValueException;
import io.jooby.exception.TypeMismatchException;
import io.jooby.value.ConversionHint;
import io.jooby.value.Value;
import io.jooby.value.ValueFactory;

public class ArrayValue implements Value {
  private final ValueFactory factory;

  private final String name;

  private final List<Value> list = new ArrayList<>(5);

  public ArrayValue(ValueFactory factory, String name) {
    this.factory = factory;
    this.name = name;
  }

  @Override
  public String name() {
    return name;
  }

  public ArrayValue add(Value value) {
    this.list.add(value);
    return this;
  }

  public ArrayValue add(List<String> values) {
    for (String value : values) {
      add(value);
    }
    return this;
  }

  public ArrayValue add(String value) {
    return this.add(new SingleValue(factory, name, value));
  }

  @Override
  public @NonNull Value get(int index) {
    try {
      return list.get(index);
    } catch (IndexOutOfBoundsException x) {
      return new MissingValue(factory, name + "[" + index + "]");
    }
  }

  @Override
  public @NonNull Value get(@NonNull String name) {
    return new MissingValue(factory, this.name + "." + name);
  }

  @Override
  public Value getOrDefault(@NonNull String name, @NonNull String defaultValue) {
    return Value.value(factory, this.name + "." + name, defaultValue);
  }

  @Override
  public int size() {
    return list.size();
  }

  @Override
  public @NonNull String value() {
    String name = name();
    throw new TypeMismatchException(name == null ? getClass().getSimpleName() : name, String.class);
  }

  @Override
  public String toString() {
    return list.toString();
  }

  @Override
  public @NonNull Iterator<Value> iterator() {
    return list.iterator();
  }

  @NonNull @Override
  public <T> T to(@NonNull Class<T> type) {
    return factory.convert(type, list.get(0), ConversionHint.Strict);
  }

  @Nullable @Override
  public <T> T toNullable(@NonNull Class<T> type) {
    return list.isEmpty() ? null : factory.convert(type, list.get(0), ConversionHint.Nullable);
  }

  @NonNull @Override
  public <T> List<T> toList(@NonNull Class<T> type) {
    return collect(new ArrayList<>(this.list.size()), type);
  }

  @NonNull @Override
  public <T> Optional<T> toOptional(@NonNull Class<T> type) {
    try {
      return Optional.ofNullable(toNullable(type));
    } catch (MissingValueException x) {
      return Optional.empty();
    }
  }

  @NonNull @Override
  public <T> Set<T> toSet(@NonNull Class<T> type) {
    return collect(new LinkedHashSet<>(this.list.size()), type);
  }

  @Override
  public @NonNull Map<String, List<String>> toMultimap() {
    var values = new ArrayList<String>();
    list.forEach(it -> it.toMultimap().values().forEach(values::addAll));
    return Map.of(name, values);
  }

  @Override
  public @NonNull List<String> toList() {
    return switch (list.size()) {
      case 0 -> List.of();
      case 1 -> List.of(list.get(0).value());
      case 2 -> List.of(list.get(0).value(), list.get(1).value());
      case 3 -> List.of(list.get(0).value(), list.get(1).value(), list.get(2).value());
      default -> collect(new ArrayList<>(list.size()), String.class);
    };
  }

  @Override
  public @NonNull Set<String> toSet() {
    return switch (list.size()) {
      case 0 -> Set.of();
      case 1 -> Set.of(list.get(0).value());
      default -> collect(new LinkedHashSet<>(list.size()), String.class);
    };
  }

  private <T, C extends Collection<T>> C collect(C collection, Class<T> type) {
    for (var node : list) {
      if (type == String.class) {
        //noinspection unchecked
        collection.add((T) node.value());
      } else {
        collection.add(node.to(type));
      }
    }
    return collection;
  }
}
