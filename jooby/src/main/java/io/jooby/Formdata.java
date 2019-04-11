/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.internal.HashValue;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * Formdata class for direct MVC parameter provisioning.
 *
 * HTTP request must be encoded as {@link MediaType#FORM_URLENCODED}.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface Formdata extends Value {

  /**
   * Add a form field.
   *
   * @param path Form name/path.
   * @param value Form value.
   * @return This formdata.
   */
  @Nonnull Formdata put(@Nonnull String path, @Nonnull Value value);

  /**
   * Add a form field.
   *
   * @param path Form name/path.
   * @param value Form value.
   * @return This formdata.
   */
  @Nonnull Formdata put(@Nonnull String path, @Nonnull String value);

  /**
   * Add a form field.
   *
   * @param path Form name/path.
   * @param values Form values.
   * @return This formdata.
   */
  @Nonnull Formdata put(@Nonnull String path, @Nonnull Collection<String> values);

  /**
   * Creates a formdata object.
   *
   * @return Formdata.
   */
  static @Nonnull Formdata create() {
    return new HashValue(null).setObjectType("formdata");
  }
}
