/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.Context;
import io.jooby.ValueNode;
import io.jooby.exception.MissingValueException;
import io.jooby.exception.TypeMismatchException;

public class ArrayValue implements ValueNode {
  private final Context ctx;

  private final String name;

  private final List<ValueNode> list = new ArrayList<>(5);

  public ArrayValue(Context ctx, String name) {
    this.ctx = ctx;
    this.name = name;
  }

  @Override
  public String name() {
    return name;
  }

  public ArrayValue add(ValueNode value) {
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
    return this.add(new SingleValue(ctx, name, value));
  }

  @Override
  public ValueNode get(@NonNull int index) {
    try {
      return list.get(index);
    } catch (IndexOutOfBoundsException x) {
      return new MissingValue(name + "[" + index + "]");
    }
  }

  @Override
  public ValueNode get(@NonNull String name) {
    return new MissingValue(this.name + "." + name);
  }

  @Override
  public int size() {
    return list.size();
  }

  @Override
  public String value() {
    String name = name();
    throw new TypeMismatchException(name == null ? getClass().getSimpleName() : name, String.class);
  }

  @Override
  public String toString() {
    return list.toString();
  }

  @Override
  public Iterator<ValueNode> iterator() {
    return list.iterator();
  }

  @NonNull @Override
  public <T> T to(@NonNull Class<T> type) {
    return ctx.convert(list.get(0), type);
  }

  @Nullable @Override
  public <T> T toNullable(@NonNull Class<T> type) {
    return list.isEmpty() ? null : ctx.convertOrNull(list.get(0), type);
  }

  @NonNull @Override
  public <T> List<T> toList(@NonNull Class<T> type) {
    return collect(new ArrayList<>(this.list.size()), type);
  }

  @NonNull @Override
  public <T> Optional<T> toOptional(@NonNull Class<T> type) {
    try {
      return Optional.ofNullable(to(type));
    } catch (MissingValueException x) {
      return Optional.empty();
    }
  }

  @NonNull @Override
  public <T> Set<T> toSet(@NonNull Class<T> type) {
    return collect(new LinkedHashSet<>(this.list.size()), type);
  }

  @Override
  public Map<String, List<String>> toMultimap() {
    List<String> values = new ArrayList<>();
    list.stream().forEach(it -> it.toMultimap().values().forEach(values::addAll));
    return Collections.singletonMap(name, values);
  }

  @Override
  public List<String> toList() {
    return collect(new ArrayList<>(), String.class);
  }

  @Override
  public Set<String> toSet() {
    return collect(new LinkedHashSet<>(), String.class);
  }

  private <T, C extends Collection<T>> C collect(C collection, Class<T> type) {
    for (ValueNode node : list) {
      collection.add(node.to(type));
    }
    return collection;
  }
}
