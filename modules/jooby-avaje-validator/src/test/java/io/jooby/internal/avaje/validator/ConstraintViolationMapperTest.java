/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.avaje.validator;

import static io.jooby.validation.ValidationResult.ErrorType.FIELD;
import static io.jooby.validation.ValidationResult.ErrorType.GLOBAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;

import io.avaje.validation.ConstraintViolation;
import io.avaje.validation.ConstraintViolationException;
import io.jooby.StatusCode;
import io.jooby.validation.ValidationResult;

@SuppressWarnings("unchecked")
class ConstraintViolationMapperTest {

  @Test
  void shouldReturnNullForNonConstraintViolationExceptions() {
    ConstraintViolationMapper mapper =
        new ConstraintViolationMapper(StatusCode.UNPROCESSABLE_ENTITY, "Validation failed");

    ValidationResult result =
        mapper.toResult(StatusCode.BAD_REQUEST, new RuntimeException("Other error"));

    assertNull(result);
  }

  @Test
  void shouldMapGlobalAndFieldViolationsToValidationResult() {
    ConstraintViolationMapper mapper =
        new ConstraintViolationMapper(StatusCode.UNPROCESSABLE_ENTITY, "Validation failed");

    ConstraintViolation globalViolation = mock(ConstraintViolation.class);
    when(globalViolation.path()).thenReturn("");
    when(globalViolation.message()).thenReturn("Invalid configuration");

    ConstraintViolation fieldViolation1 = mock(ConstraintViolation.class);
    when(fieldViolation1.path()).thenReturn("user.email");
    when(fieldViolation1.message()).thenReturn("Email cannot be null");

    ConstraintViolation fieldViolation2 = mock(ConstraintViolation.class);
    when(fieldViolation2.path()).thenReturn("user.email");
    when(fieldViolation2.message()).thenReturn("Email must be valid");

    ConstraintViolationException exception = mock(ConstraintViolationException.class);

    Set violations = Set.of(globalViolation, fieldViolation1, fieldViolation2);
    when(exception.violations()).thenReturn(violations);

    ValidationResult result = mapper.toResult(StatusCode.BAD_REQUEST, exception);

    assertNotNull(result);
    assertEquals("Validation failed", result.getTitle());
    assertEquals(StatusCode.UNPROCESSABLE_ENTITY.value(), result.getStatus());
    assertEquals(2, result.getErrors().size());

    ValidationResult.Error globalError =
        result.getErrors().stream().filter(e -> e.type() == GLOBAL).findFirst().orElseThrow();

    assertNull(globalError.field());
    assertEquals(1, globalError.messages().size());
    assertTrue(globalError.messages().contains("Invalid configuration"));

    ValidationResult.Error fieldError =
        result.getErrors().stream().filter(e -> e.type() == FIELD).findFirst().orElseThrow();

    assertEquals("user.email", fieldError.field());
    assertEquals(2, fieldError.messages().size());
    assertTrue(fieldError.messages().contains("Email cannot be null"));
    assertTrue(fieldError.messages().contains("Email must be valid"));
  }
}
