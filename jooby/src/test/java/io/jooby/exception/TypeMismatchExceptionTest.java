/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Type;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.StatusCode;
import io.jooby.problem.HttpProblem;

class TypeMismatchExceptionTest {

  @Test
  @DisplayName("Verify constructor with cause stores name, message, and cause correctly")
  void testConstructorWithCause() {
    String name = "age";
    Type type = int.class;
    Throwable cause = new NumberFormatException("For input string: \"abc\"");

    TypeMismatchException exception = new TypeMismatchException(name, type, cause);

    assertEquals(name, exception.getName());
    assertEquals("Cannot convert value: 'age', to: 'int'", exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  @DisplayName("Verify constructor without cause sets cause to null")
  void testConstructorWithoutCause() {
    String name = "active";
    Type type = boolean.class;

    TypeMismatchException exception = new TypeMismatchException(name, type);

    assertEquals(name, exception.getName());
    assertEquals("Cannot convert value: 'active', to: 'boolean'", exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  @DisplayName("Verify toHttpProblem returns a correctly populated HttpProblem instance")
  void testToHttpProblem() {
    String name = "price";
    Type type = double.class;
    TypeMismatchException exception = new TypeMismatchException(name, type);

    HttpProblem problem = exception.toHttpProblem();

    // BadRequestException usually defaults to 400
    assertEquals(StatusCode.BAD_REQUEST.value(), problem.getStatus());
    assertEquals("Type Mismatch", problem.getTitle());
    assertEquals("Cannot convert value: 'price', to: 'double'", problem.getDetail());
  }
}
