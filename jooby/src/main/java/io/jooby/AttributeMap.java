/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;

public class AttributeMap {

  private Map<AttributeKey, Object> map;

  public AttributeMap(Map<AttributeKey, Object> map) {
    this.map = map;
  }

  public AttributeMap() {
    this(new HashMap<>());
  }

  public boolean contains(@Nonnull AttributeKey<?> key) {
    return map.containsKey(key);
  }

  public @Nullable <V> V computeIfAbsent(@Nonnull AttributeKey<V> key,
      @Nonnull Throwing.Function<AttributeKey<V>, V> mappingFunction) {
    Function fn = mappingFunction;
    return (V) map.computeIfAbsent(key, fn);
  }

  public @Nonnull <V> V get(@Nonnull AttributeKey<V> key) {
    V value = (V) map.get(key);
    if (value == null) {
      throw new NoSuchElementException(key.toString());
    }
    return value;
  }

  public @Nullable <V> V getOrNull(@Nonnull AttributeKey<V> key) {
    return (V) map.get(key);
  }

  public @Nonnull <V> V getOrDefault(@Nonnull AttributeKey<V> key, @Nonnull V value) {
    return (V) map.getOrDefault(key, value);
  }

  public @Nullable <V> V put(@Nonnull AttributeKey<V> key, @Nonnull V value) {
    return (V) map.put(key, value);
  }

  public @Nullable <V> V putIfAbsent(@Nonnull AttributeKey<V> key, @Nonnull V value) {
    return (V) map.putIfAbsent(key, value);
  }

  public @Nullable <V> V remove(@Nonnull AttributeKey<V> key) {
    return (V) map.remove(key);
  }

  public @Nonnull Set<AttributeKey> keySet() {
    return map.keySet();
  }

  public Map<String, Object> toMap() {
    Map<String, Object> result = new HashMap<>();
    for (Map.Entry<AttributeKey, Object> entry : map.entrySet()) {
      AttributeKey key = entry.getKey();
      if (key.getName() != null) {
        result.put(key.getName(), entry.getValue());
      }
    }
    return result;
  }
}
