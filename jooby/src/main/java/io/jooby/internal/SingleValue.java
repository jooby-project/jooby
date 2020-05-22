/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Context;
import io.jooby.ValueNode;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

public class SingleValue implements ValueNode {

  private final Context ctx;

  private final String name;

  private String value;

  public SingleValue(Context ctx, String name, String value) {
    this.ctx = ctx;
    this.name = name;
    this.value = value;
  }

  @Override public String name() {
    return name;
  }

  @Override public ValueNode get(@Nonnull int index) {
    return index == 0 ? this : get(Integer.toString(index));
  }

  @Override public ValueNode get(@Nonnull String name) {
    return new MissingValue(this.name + "." + name);
  }

  @Override public int size() {
    return 1;
  }

  @Override public String value() {
    return value;
  }

  @Override public String toString() {
    return value;
  }

  @Override public Iterator<ValueNode> iterator() {
    return Collections.<ValueNode>singletonList(this).iterator();
  }

  @Nonnull @Override public <T> List<T> toList(@Nonnull Class<T> type) {
    return Collections.singletonList(to(type));
  }

  @Nonnull @Override public <T> Set<T> toSet(@Nonnull Class<T> type) {
    return Collections.singleton(to(type));
  }

  @Nonnull @Override public <T> Optional<T> toOptional(@Nonnull Class<T> type) {
    return Optional.of(to(type));
  }

  @Nonnull @Override public <T> T to(@Nonnull Class<T> type) {
    return ctx.convert(this, type);
  }

  @Override public Map<String, List<String>> toMultimap() {
    return singletonMap(name, singletonList(value));
  }

  @Override public List<String> toList() {
    return singletonList(value);
  }

  @Override public Set<String> toSet() {
    return singleton(value);
  }
}
