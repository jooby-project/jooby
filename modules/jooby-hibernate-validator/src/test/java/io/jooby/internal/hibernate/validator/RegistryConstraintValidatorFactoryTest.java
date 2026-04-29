/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.hibernate.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jooby.Registry;
import io.jooby.exception.RegistryException;
import jakarta.validation.ConstraintValidator;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes"})
class RegistryConstraintValidatorFactoryTest {

  @Mock private Registry registry;

  private RegistryConstraintValidatorFactory factory;

  @BeforeEach
  void setUp() {
    factory = new RegistryConstraintValidatorFactory(registry);
  }

  @Test
  void shouldReturnValidatorFromRegistry() {
    ConstraintValidator mockValidator = mock(ConstraintValidator.class);
    when(registry.require(ConstraintValidator.class)).thenReturn(mockValidator);

    ConstraintValidator<?, ?> result = factory.getInstance(ConstraintValidator.class);

    assertEquals(mockValidator, result);
  }

  @Test
  void shouldReturnNullWhenRegistryThrowsException() {
    when(registry.require(ConstraintValidator.class))
        .thenThrow(new RegistryException("Bean not found"));

    ConstraintValidator<?, ?> result = factory.getInstance(ConstraintValidator.class);

    // Returning null allows Hibernate Validator to fall back to its default factory
    assertNull(result);
  }

  @Test
  void releaseInstanceShouldDoNothingIfValidatorIsNotAutoCloseable() {
    ConstraintValidator mockValidator = mock(ConstraintValidator.class);

    // If it's not AutoCloseable, the method should just exit silently
    assertDoesNotThrow(() -> factory.releaseInstance(mockValidator));
  }

  @Test
  void releaseInstanceShouldCallCloseIfValidatorIsAutoCloseable() throws Exception {
    CloseableValidator mockValidator = mock(CloseableValidator.class);

    factory.releaseInstance(mockValidator);

    verify(mockValidator).close();
  }

  @Test
  void releaseInstanceShouldCatchAndLogExceptionsThrownDuringClose() throws Exception {
    CloseableValidator mockValidator = mock(CloseableValidator.class);
    doThrow(new RuntimeException("Simulated close failure")).when(mockValidator).close();

    // The exception should be caught and logged at debug level, not bubbled up
    assertDoesNotThrow(() -> factory.releaseInstance(mockValidator));
  }

  /**
   * Helper interface to create a mock that is both a ConstraintValidator and AutoCloseable,
   * satisfying the branch condition in releaseInstance.
   */
  private interface CloseableValidator
      extends ConstraintValidator<java.lang.annotation.Annotation, Object>, AutoCloseable {}
}
