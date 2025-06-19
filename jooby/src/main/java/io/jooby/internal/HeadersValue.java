/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Value;
import io.jooby.value.ValueFactory;

public class HeadersValue extends HashValue implements Value {

  public HeadersValue(ValueFactory valueFactory) {
    super(valueFactory);
  }

  @Override
  protected Map<String, Value> hash() {
    if (hash == EMPTY) {
      hash = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }
    return hash;
  }

  @NonNull @Override
  public Map<String, String> toMap() {
    Map<String, String> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    toMultimap().forEach((k, v) -> map.put(k, v.get(0)));
    return map;
  }

  @NonNull @Override
  public Map<String, List<String>> toMultimap() {
    Map<String, List<String>> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    Set<Map.Entry<String, Value>> entries = hash.entrySet();
    for (Map.Entry<String, Value> entry : entries) {
      Value value = entry.getValue();
      result.putAll(value.toMultimap());
    }
    return result;
  }
}
