/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;

/**
 * Missing exception. Used when a required attribute/value is missing.
 *
 * @since 2.0.0
 * @author edgar
 */
public class MissingValueException extends BadRequestException {
  private final String name;

  /**
   * Creates a missing exception.
   *
   * @param name Parameter/attribute name.
   */
  public MissingValueException(@Nonnull String name) {
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
}
