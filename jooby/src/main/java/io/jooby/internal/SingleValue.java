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
