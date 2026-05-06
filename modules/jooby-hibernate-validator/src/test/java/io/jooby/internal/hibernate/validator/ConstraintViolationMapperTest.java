/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.hibernate.validator;

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

import io.jooby.StatusCode;
import io.jooby.validation.ValidationResult;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

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

    // Mock Paths
    Path rootPath = mock(Path.class);
    when(rootPath.toString()).thenReturn("");

    Path fieldPath = mock(Path.class);
    when(fieldPath.toString()).thenReturn("user.email");

    // Mock Violations
    ConstraintViolation<Object> globalViolation = mock(ConstraintViolation.class);
    when(globalViolation.getPropertyPath()).thenReturn(rootPath);
    when(globalViolation.getMessage()).thenReturn("Invalid configuration");

    ConstraintViolation<Object> fieldViolation1 = mock(ConstraintViolation.class);
    when(fieldViolation1.getPropertyPath()).thenReturn(fieldPath);
    when(fieldViolation1.getMessage()).thenReturn("Email cannot be null");

    ConstraintViolation<Object> fieldViolation2 = mock(ConstraintViolation.class);
    when(fieldViolation2.getPropertyPath()).thenReturn(fieldPath);
    when(fieldViolation2.getMessage()).thenReturn("Email must be valid");

    // Create Exception with Violations
    Set<ConstraintViolation<?>> violations =
        Set.of(globalViolation, fieldViolation1, fieldViolation2);
    ConstraintViolationException exception = new ConstraintViolationException(violations);

    // Execute
    ValidationResult result = mapper.toResult(StatusCode.BAD_REQUEST, exception);

    // Verify Root Properties
    assertNotNull(result);
    assertEquals("Validation failed", result.getTitle());
    assertEquals(StatusCode.UNPROCESSABLE_ENTITY.value(), result.getStatus());
    assertEquals(2, result.getErrors().size());

    // Verify Error mapping logic
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
