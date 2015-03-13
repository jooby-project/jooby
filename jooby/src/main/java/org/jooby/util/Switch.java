/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.util;

import static java.util.Objects.requireNonNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

/**
 * A programmatic switch:
 *
 * <pre>
 *   String var = ...;
 *   Out out = Switch.newSwitch(var)
 *    .when("A", () {@literal ->} doA())
 *    .when("B", () {@literal ->} doB())
 *    .value();
 * </pre>
 *
 * @author edgar
 * @param <In> Input type.
 * @param <Out> Output type.
 */
public class Switch<In, Out> {

  /** Switch table. */
  private final Map<Predicate<In>, ExSupplier<Out>> strategies = new LinkedHashMap<>();

  /** Input value. */
  private In value;

  /**
   * Creates a new switch.
   *
   * @param value Input value.
   */
  private Switch(final @Nonnull In value) {
    this.value = requireNonNull(value, "Input value is required.");
  }

  /**
   * Append a new option.
   *
   * @param value A value to test.
   * @param result A constant value.
   * @return This switch.
   */
  public @Nonnull Switch<In, Out> when(final @Nonnull In value, final @Nonnull Out result) {
    return when(value, () -> result);
  }

  /**
   * Append a new option.
   *
   * @param predicate A predicate to test.
   * @param result A constant value.
   * @return This switch.
   */
  public @Nonnull Switch<In, Out> when(final @Nonnull Predicate<In> predicate, final @Nonnull Out result) {
    return when(predicate, () -> result);
  }

  /**
   * Append a new option.
   *
   * @param value A value to test.
   * @param fn A result supplier value.
   * @return This switch.
   */
  public @Nonnull Switch<In, Out> when(final @Nonnull In value, final @Nonnull ExSupplier<Out> fn) {
    return when(value::equals, fn);
  }

  /**
   * Append a new option.
   *
   * @param predicate A predicate to test.
   * @param fn A result supplier value.
   * @return This switch.
   */
  public @Nonnull Switch<In, Out> when(final @Nonnull Predicate<In> predicate, final @Nonnull ExSupplier<Out> fn) {
    requireNonNull(value, "A value is required.");
    requireNonNull(fn, "A function is required.");
    strategies.put(predicate, fn);
    return this;
  }

  /**
   * Test all the switches and find the first who best matches the input value.
   *
   * @return A value or empty optional.
   * @throws Exception If something goes wrong.
   */
  public @Nonnull Optional<Out> value() throws Exception {
    for (Entry<Predicate<In>, ExSupplier<Out>> entry : strategies.entrySet()) {
      if (entry.getKey().test(value)) {
        return Optional.ofNullable(entry.getValue().get());
      }
    }
    return Optional.empty();
  }

  /**
   * Creates a new switch.
   *
   * @param value An input value.
   * @param <In> Input value.
   * @param <Out> Output value.
   * @return A new switch.
   */
  public static @Nonnull <In, Out>  Switch<In, Out> newSwitch(final @Nonnull In value) {
    return new Switch<In, Out>(value);
  }

  /**
   * Creates a new string switch.
   *
   * @param value An input value.
   * @param <Out> Output value.
   * @return A new switch.
   */
  public static @Nonnull <Out> Switch<String, Out> newSwitch(final @Nonnull String value) {
    return new Switch<String, Out>(value);
  }
}
