/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
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
