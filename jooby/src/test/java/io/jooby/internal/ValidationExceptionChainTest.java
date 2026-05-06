/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.jooby.StatusCode;
import io.jooby.validation.ValidationExceptionMapper;
import io.jooby.validation.ValidationResult;

class ValidationExceptionChainTest {

  @Test
  void shouldReturnResultFromFirstApplicableMapper() {
    ValidationExceptionChain chain = new ValidationExceptionChain();

    ValidationExceptionMapper mapper1 = mock(ValidationExceptionMapper.class);
    ValidationExceptionMapper mapper2 = mock(ValidationExceptionMapper.class);
    ValidationExceptionMapper mapper3 = mock(ValidationExceptionMapper.class);

    Exception cause = new RuntimeException("Test error");
    ValidationResult expectedResult =
        new ValidationResult("Custom title", 422, Collections.emptyList());

    // Mapper 1 returns null (cannot handle)
    when(mapper1.toResult(StatusCode.UNPROCESSABLE_ENTITY, cause)).thenReturn(null);
    // Mapper 2 returns a valid result
    when(mapper2.toResult(StatusCode.UNPROCESSABLE_ENTITY, cause)).thenReturn(expectedResult);

    // Chaining add methods
    chain.add(mapper1).add(mapper2).add(mapper3);

    ValidationResult result = chain.toResult(StatusCode.UNPROCESSABLE_ENTITY, cause);

    assertSame(expectedResult, result);
    verify(mapper1).toResult(StatusCode.UNPROCESSABLE_ENTITY, cause);
    verify(mapper2).toResult(StatusCode.UNPROCESSABLE_ENTITY, cause);
    // Mapper 3 should never be called since Mapper 2 handled it
    verifyNoInteractions(mapper3);
  }

  @Test
  void shouldReturnDefaultResultWhenNoMapperHandlesItAndStatusCodeIsClientError() {
    ValidationExceptionChain chain = new ValidationExceptionChain();
    Exception cause = new IllegalArgumentException("Invalid input provided");

    ValidationResult result = chain.toResult(StatusCode.BAD_REQUEST, cause);

    assertNotNull(result);
    assertEquals("Validation failed", result.getTitle());
    assertEquals(400, result.getStatus());

    assertEquals(1, result.getErrors().size());
    ValidationResult.Error error = result.getErrors().get(0);
    assertNull(error.field());
    assertEquals(ValidationResult.ErrorType.GLOBAL, error.type());
    assertEquals(List.of("Invalid input provided"), error.messages());
  }

  @Test
  void shouldReturnDefaultResultUsingClassNameWhenExceptionHasNoMessage() {
    ValidationExceptionChain chain = new ValidationExceptionChain();
    // Exception without a message
    Exception cause = new NullPointerException();

    ValidationResult result = chain.toResult(StatusCode.BAD_REQUEST, cause);

    assertNotNull(result);
    assertEquals("Validation failed", result.getTitle());
    assertEquals(400, result.getStatus());

    assertEquals(1, result.getErrors().size());
    ValidationResult.Error error = result.getErrors().get(0);
    assertNull(error.field());
    assertEquals(ValidationResult.ErrorType.GLOBAL, error.type());
    // Fallback to the class simple name
    assertEquals(List.of("NullPointerException"), error.messages());
  }

  @Test
  void shouldReturnNullWhenStatusCodeIsServerError() {
    ValidationExceptionChain chain = new ValidationExceptionChain();
    Exception cause = new IllegalStateException("Database connection failed");

    // >= 500 status code
    assertNull(chain.toResult(StatusCode.SERVER_ERROR, cause));
  }
}
