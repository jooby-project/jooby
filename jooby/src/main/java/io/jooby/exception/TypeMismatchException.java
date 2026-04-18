/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.exception;

import java.lang.reflect.Type;

import io.jooby.problem.HttpProblem;

/**
 * Type mismatch exception. Used when a value can't be converted to the required type.
 *
 * @since 2.0.0
 * @author edgar
 */
public class TypeMismatchException extends BadRequestException {
  /** Parameter/field name. */
  private final String name;

  /**
   * Creates a type mismatch error.
   *
   * @param name Parameter/attribute name.
   * @param type Parameter/attribute type.
   * @param cause Cause.
   */
  public TypeMismatchException(String name, Type type, Throwable cause) {
    super("Cannot convert value: '" + name + "', to: '" + type.getTypeName() + "'", cause);
    this.name = name;
  }

  /**
   * Creates a type mismatch error.
   *
   * @param name Parameter/attribute name.
   * @param type Parameter/attribute type.
   */
  public TypeMismatchException(String name, Type type) {
    this(name, type, null);
  }

  /**
   * Parameter/attribute name.
   *
   * @return Parameter/attribute name.
   */
  public String getName() {
    return name;
  }

  @Override
  public HttpProblem toHttpProblem() {
    return HttpProblem.valueOf(statusCode, "Type Mismatch", getMessage());
  }
}
