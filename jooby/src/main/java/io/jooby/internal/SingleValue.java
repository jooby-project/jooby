/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Value;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

public class SingleValue implements Value {

  private final String name;

  private final String value;

  public SingleValue(String name, String value) {
    this.name = name;
    this.value = value;
  }

  @Override public String name() {
    return name;
  }

  @Override public Value get(@Nonnull int index) {
    return index == 0 ? this : get(Integer.toString(index));
  }

  @Override public Value get(@Nonnull String name) {
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

  @Override public Iterator<Value> iterator() {
    return Collections.<Value>singletonList(this).iterator();
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
