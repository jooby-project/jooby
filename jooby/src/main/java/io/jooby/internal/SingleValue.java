/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.ValueNode;
import io.jooby.exception.TypeMismatchException;
import io.jooby.value.ValueFactory;

public class SingleValue implements ValueNode {

  private final ValueFactory factory;

  private final String name;

  private String value;

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
  public @NonNull ValueNode get(int index) {
    return index == 0 ? this : get(Integer.toString(index));
  }

  @Override
  public @NonNull ValueNode get(@NonNull String name) {
    return new MissingValue(this.name + "." + name);
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
  public Iterator<ValueNode> iterator() {
    return Collections.<ValueNode>singletonList(this).iterator();
  }

  @NonNull @Override
  public <T> List<T> toList(@NonNull Class<T> type) {
    return Collections.singletonList(to(type));
  }

  @NonNull @Override
  public <T> Set<T> toSet(@NonNull Class<T> type) {
    return Collections.singleton(to(type));
  }

  @NonNull @Override
  public <T> Optional<T> toOptional(@NonNull Class<T> type) {
    return Optional.of(to(type));
  }

  @NonNull @Override
  public <T> T to(@NonNull Class<T> type) {
    var result = ValueConverters.convert(this, type, factory);
    if (result == null) {
      throw new TypeMismatchException(name(), type);
    }
    return (T) result;
  }

  @Nullable @Override
  public <T> T toNullable(@NonNull Class<T> type) {
    return ValueConverters.convert(this, type, factory);
  }

  @Override
  public Map<String, List<String>> toMultimap() {
    return singletonMap(name, singletonList(value));
  }

  @Override
  public List<String> toList() {
    return singletonList(value);
  }

  @Override
  public Set<String> toSet() {
    return singleton(value);
  }
}
