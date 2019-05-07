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
import java.lang.reflect.Type;

/**
 * Type mismatch exception. Used when a value can't be converted to the required type.
 *
 * @since 2.0.0
 * @author edgar
 */
public class TypeMismatchException extends BadRequestException {
  private final String name;

  /**
   * Creates a type mismatch error.
   *
   * @param name Parameter/attribute name.
   * @param type Parameter/attribute type.
   * @param cause Cause.
   */
  public TypeMismatchException(@Nonnull String name, @Nonnull Type type, @Nonnull Throwable cause) {
    super("Cannot convert value: '" + name + "', to: '" + type.getTypeName() + "'", cause);
    this.name = name;
  }

  /**
   * Creates a type mismatch error.
   *
   * @param name Parameter/attribute name.
   * @param type Parameter/attribute type.
   */
  public TypeMismatchException(@Nonnull String name, @Nonnull Type type) {
    this(name, type, null);
  }

  /**
   * Parameter/attribute name.
   *
   * @return Parameter/attribute name.
   */
  public @Nonnull String getName() {
    return name;
  }
}
