/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.hibernate.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import io.jooby.Jooby;
import io.jooby.exception.RegistryException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorFactory;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class CompositeConstraintValidatorFactoryTest {

  @Mock private Jooby registry;
  @Mock private Logger logger;
  @Mock private ConstraintValidatorFactory defaultFactory;

  private CompositeConstraintValidatorFactory compositeFactory;

  @BeforeEach
  void setUp() {
    when(registry.getLog()).thenReturn(logger);
    compositeFactory = new CompositeConstraintValidatorFactory(registry, defaultFactory);
  }

  @Test
  void shouldDelegateBuiltInHibernateClassToDefaultFactory() {
    // Tests the isBuiltIn() true branch for "org.hibernate.validator"
    Class key = org.hibernate.validator.HibernateValidator.class;
    ConstraintValidator expected = mock(ConstraintValidator.class);
    when(defaultFactory.getInstance(key)).thenReturn(expected);

    ConstraintValidator result = compositeFactory.getInstance(key);

    assertEquals(expected, result);
    verify(defaultFactory).getInstance(key);
  }

  @Test
  void shouldDelegateBuiltInJakartaClassToDefaultFactory() {
    // Tests the isBuiltIn() true branch for "jakarta.validation"
    Class key = jakarta.validation.ConstraintValidator.class;
    ConstraintValidator expected = mock(ConstraintValidator.class);
    when(defaultFactory.getInstance(key)).thenReturn(expected);

    ConstraintValidator result = compositeFactory.getInstance(key);

    assertEquals(expected, result);
    verify(defaultFactory).getInstance(key);
  }

  @Test
  void shouldReturnInstanceFromAddedFactory() {
    ConstraintValidatorFactory customFactory = mock(ConstraintValidatorFactory.class);
    compositeFactory.add(customFactory);

    Class key = CustomValidator.class;
    ConstraintValidator expected = mock(ConstraintValidator.class);

    // The custom factory successfully provides the instance
    when(customFactory.getInstance(key)).thenReturn(expected);

    ConstraintValidator result = compositeFactory.getInstance(key);

    assertEquals(expected, result);
    // Ensure the default factory wasn't called since the custom one handled it
    verify(defaultFactory, never()).getInstance(any());
  }

  @Test
  void shouldFallbackToDefaultFactoryIfNoAddedFactoryReturnsInstance() {
    ConstraintValidatorFactory customFactory = mock(ConstraintValidatorFactory.class);
    compositeFactory.add(customFactory);

    Class key = CustomValidator.class;

    // 1. Custom factory returns null
    when(customFactory.getInstance(key)).thenReturn(null);

    // 2. The internal RegistryConstraintValidatorFactory throws/returns null
    when(registry.require(key)).thenThrow(new RegistryException("Not found"));

    ConstraintValidator expected = mock(ConstraintValidator.class);
    when(defaultFactory.getInstance(key)).thenReturn(expected);

    // 3. It should drop down to the fallback
    ConstraintValidator result = compositeFactory.getInstance(key);

    assertEquals(expected, result);
    verify(defaultFactory).getInstance(key);
  }

  @Test
  void releaseInstanceShouldDelegateToDefaultFactoryForBuiltIn() throws Exception {
    // We instantiate a real built-in Hibernate class via reflection so its
    // getClass().getName() naturally starts with "org.hibernate.validator"
    Class<?> clazz =
        Class.forName("org.hibernate.validator.internal.constraintvalidators.bv.NotNullValidator");
    ConstraintValidator instance =
        (ConstraintValidator) clazz.getDeclaredConstructor().newInstance();

    compositeFactory.releaseInstance(instance);

    verify(defaultFactory).releaseInstance(instance);
  }

  @Test
  void releaseInstanceShouldDoNothingIfCustomAndNotCloseable() {
    ConstraintValidator instance = new CustomValidator();

    assertDoesNotThrow(() -> compositeFactory.releaseInstance(instance));

    // Shouldn't attempt to release via default factory for custom classes
    verify(defaultFactory, never()).releaseInstance(any());
  }

  @Test
  void releaseInstanceShouldCloseIfCustomAndCloseable() throws Exception {
    CloseableValidator instance = mock(CloseableValidator.class);

    compositeFactory.releaseInstance(instance);

    verify(instance).close();
    verify(defaultFactory, never()).releaseInstance(any());
  }

  @Test
  void releaseInstanceShouldLogExceptionIfCloseFails() throws Exception {
    CloseableValidator instance = mock(CloseableValidator.class);
    Exception error = new RuntimeException("Close error");
    doThrow(error).when(instance).close();

    assertDoesNotThrow(() -> compositeFactory.releaseInstance(instance));

    // Verifies the exception is safely swallowed and logged at debug level
    verify(logger).debug("Failed to release constraint", error);
  }

  // --- Dummy Test Classes to satisfy generics and interfaces ---

  private static class CustomValidator
      implements ConstraintValidator<jakarta.validation.constraints.NotNull, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
      return true;
    }
  }

  private interface CloseableValidator
      extends ConstraintValidator<jakarta.validation.constraints.NotNull, String>, AutoCloseable {}
}
