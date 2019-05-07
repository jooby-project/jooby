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

import io.jooby.MissingValueException;
import io.jooby.Value;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MissingValue implements Value {
  private String name;

  public MissingValue(String name) {
    this.name = name;
  }

  @Override public String name() {
    return name;
  }

  @Override public Value get(@Nonnull String name) {
    return this.name.equals(name) ? this : new MissingValue(this.name + "." + name);
  }

  @Override public Value get(@Nonnull int index) {
    return new MissingValue(this.name + "[" + index + "]");
  }

  @Override public String value() {
    throw new MissingValueException(name);
  }

  @Override public Map<String, List<String>> toMultimap() {
    return Collections.emptyMap();
  }
}
