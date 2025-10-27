/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.exception;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Missing exception. Used when a required attribute/value is missing.
 *
 * @since 2.0.0
 * @author edgar
 */
public class MissingValueException extends BadRequestException {
  /** Parameter/field name. */
  private final String name;

  /**
   * Creates a missing exception.
   *
   * @param name Parameter/attribute name.
   */
  public MissingValueException(@NonNull String name) {
    super("Missing value: '" + name + "'");
    this.name = name;
  }

  /**
   * Parameter/attribute name.
   *
   * @return Parameter/attribute name.
   */
  public String getName() {
    return name;
  }

  /**
   * Check if the given value is null and throw a {@link MissingValueException} exception.
   *
   * @param name Attribute's name.
   * @param value Value to check.
   * @param <T> Value type.
   * @return Input value
   */
  public static <T> T requireNonNull(@NonNull String name, @Nullable T value) {
    if (value == null) {
      throw new MissingValueException(name);
    }
    return value;
  }
}
