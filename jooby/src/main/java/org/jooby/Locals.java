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

import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

/**
 * Local attributes for {@link Request} and {@link Session}.
 *
 * @author edgar
 */
public interface Locals {

  /**
   * Get a named object. If the object isn't found this method returns an empty
   * optional.
   *
   * @param name A local var's name.
   * @param <T> Target type.
   * @return A value or empty optional.
   */
  @Nonnull
  <T> Optional<T> get(final @Nonnull String name);

  /**
   * @return An immutable copy of local attributes.
   */
  @Nonnull
  Map<String, Object> attributes();

  /**
   * Test if the var name exists inside the local attributes.
   *
   * @param name A local var's name.
   * @return True, for existing locals.
   */
  default boolean isSet(final @Nonnull String name) {
    return get(name).isPresent();
  }

  /**
   * Set a local using a the given name. If a local already exists, it will be replaced
   * with the new value. Keep in mind that ONLY none null values are allowed.
   *
   * @param name A local var's name.
   * @param value A local values.
   * @return This locals.
   */
  @Nonnull Locals set(final @Nonnull String name, final @Nonnull Object value);

  /**
   * Remove a local value (if any) from session locals.
   *
   * @param name A local var's name.
   * @param <T> A local type.
   * @return Existing value or empty optional.
   */
  <T> Optional<T> unset(final String name);

  /**
   * Unset/remove all the session data.
   *
   * @return This locals.
   */
  Locals unset();

}
