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
 * Access to raw response value from {@link MockRouter} or cast response to something else.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface MockValue {
  /**
   * Raw response value.
   *
   * @return Raw response value.
   */
  @Nonnull Object value();

  /**
   * Cast response to given type.
   *
   * @param type Type to cast.
   * @param <T> Response type.
   * @return Response value.
   */
  default @Nonnull <T> T value(@Nonnull Class<T> type) {
    Object instance = value();
    if (instance == null) {
      throw new ClassCastException("Found: null, expected: " + type);
    }
    if (!type.isInstance(instance)) {
      throw new ClassCastException("Found: " + instance.getClass() + ", expected: " + type);
    }
    return type.cast(instance);
  }
}
