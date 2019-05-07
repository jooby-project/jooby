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

/**
 * Service locator pattern which may be provided by a dependency injection framework.
 *
 * @since 2.0.0
 * @author edgar
 * @see ServiceRegistry
 */
public interface Registry {
  /**
   * Provides an instance of the given type.
   *
   * @param type Object type.
   * @param <T> Object type.
   * @return Instance of this type.
   * @throws RegistryException If there was a runtime failure while providing an instance.
   */
  @Nonnull <T> T require(@Nonnull Class<T> type) throws RegistryException;

  /**
   * Provides an instance of the given type where name matches it.
   *
   * @param type Object type.
   * @param name Object name.
   * @param <T> Object type.
   * @return Instance of this type.
   * @throws RegistryException If there was a runtime failure while providing an instance.
   */
  @Nonnull <T> T require(@Nonnull Class<T> type, @Nonnull String name) throws RegistryException;
}
