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
package org.jooby;

import java.util.Optional;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import org.jooby.fn.ExSupplier;
import org.jooby.fn.Switch;

/**
 * Application's mode that let you optimize, customize or apply defaults values for services.
 *
 * <p>
 * A mode is represented by it's name. By default an application run in <strong>dev</strong> mode.
 * The <strong>dev</strong> is special and a module provider could do some special configuration for
 * development, like turning off a cache, reloading of resources, etc.
 * </p>
 * <p>
 * Same is true for no <strong>dev</strong> modes. For example, a module provider might create a
 * high performance connection pool for none dev modes, caches, etc.
 * </p>
 * <p>
 * By default a mode is set to dev, but you can change it by setting the
 * <code>application.mode</code> property to anything else.
 * </p>
 *
 * @author edgar
 * @since 0.1.0
 */
public interface Mode {

  /**
   * @return Mode's name.
   */
  @Nonnull
  String name();

  /**
   * Runs the callback function if the current mode matches the given name.
   *
   * @param name A name to test for.
   * @param fn A callback function.
   * @param <T> A resulting type.
   * @return A resulting object.
   * @throws Exception If something fails.
   */
  default <T> Optional<T> ifMode(@Nonnull final String name, @Nonnull final ExSupplier<T> fn)
      throws Exception {
    return when(name, fn).value();
  }

  /**
   * Produces a {@link Switch} of the current {@link Mode}.
   *
   * <pre>
   *   String accessKey = mode.when("dev", () {@literal ->} "1234")
   *                          .when("stage", () {@literal ->} "4321")
   *                          .when("prod", () {@literal ->} "abc")
   *                          .get();
   * </pre>
   *
   * @param name A name to test for.
   * @param fn A callback function.
   * @param <T> A resulting type.
   * @return A new switch.
   */
  @Nonnull
  default <T> Switch<String, T> when(@Nonnull final String name, @Nonnull final ExSupplier<T> fn) {
    return Switch.<T> newSwitch(name()).when(name, fn);
  }

  /**
   * Produces a {@link Switch} of the current {@link Mode}.
   *
   * <pre>
   *   String accessKey = mode.when("dev", "1234")
   *                          .when("stage", "4321")
   *                          .when("prod", "abc")
   *                          .get();
   * </pre>
   *
   * @param name A name to test for.
   * @param result A constant value to return.
   * @param <T> A resulting type.
   * @return A new switch.
   */
  @Nonnull
  default <T> Switch<String, T> when(@Nonnull final String name, @Nonnull final T result) {
    return Switch.<T> newSwitch(name()).when(name, result);
  }

  /**
   * Produces a {@link Switch} of the current {@link Mode}.
   *
   * <pre>
   *   String accessKey = mode.when("dev", () {@literal ->} "1234")
   *                          .when("stage", () {@literal ->} "4321")
   *                          .when("prod", () {@literal ->} "abc")
   *                          .get();
   * </pre>
   *
   * @param predicate A predicate to use.
   * @param result A constant value to return.
   * @param <T> A resulting type.
   * @return A new switch.
   */
  @Nonnull
  default <T> Switch<String, T> when(@Nonnull final Predicate<String> predicate,
      @Nonnull final T result) {
    return Switch.<T> newSwitch(name()).when(predicate, result);
  }
}
