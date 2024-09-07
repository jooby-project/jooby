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
import io.jooby.Context;
import io.jooby.ValueNode;

public class HeadersValue extends HashValue implements ValueNode {

  public HeadersValue(final Context ctx) {
    super(ctx);
  }

  @Override
  protected Map<String, ValueNode> hash() {
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
    Set<Map.Entry<String, ValueNode>> entries = hash.entrySet();
    for (Map.Entry<String, ValueNode> entry : entries) {
      ValueNode value = entry.getValue();
      result.putAll(value.toMultimap());
    }
    return result;
  }
}
