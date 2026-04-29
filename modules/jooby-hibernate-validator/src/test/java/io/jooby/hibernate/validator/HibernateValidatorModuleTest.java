/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.hibernate.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.typesafe.config.ConfigFactory;
import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.ServiceRegistry;
import io.jooby.StatusCode;
import io.jooby.validation.BeanValidator;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class HibernateValidatorModuleTest {

  @Mock private Jooby app;
  @Mock private ServiceRegistry services;

  @Test
  void shouldInstallWithDefaults() throws Exception {
    when(app.getServices()).thenReturn(services);
    when(app.getConfig()).thenReturn(ConfigFactory.empty());

    HibernateValidatorModule module = new HibernateValidatorModule();
    module.install(app);

    // Verify services bindings
    verify(services).put(eq(Validator.class), any(Validator.class));
    verify(services).put(eq(BeanValidator.class), any(BeanValidator.class));
    verify(services)
        .put(eq(ConstraintValidatorFactory.class), any(ConstraintValidatorFactory.class));

    // Verify default error handler is attached
    verify(app)
        .error(eq(ConstraintViolationException.class), any(ConstraintViolationHandler.class));

    // Verify application stop hook registers the factory close
    verify(app).onStop(any(AutoCloseable.class));
  }

  @Test
  void shouldInstallWithConfigurationProperties() throws Exception {
    when(app.getServices()).thenReturn(services);
    // Mimics the 'hibernate.validator' properties block
    var config = ConfigFactory.parseMap(Map.of("hibernate.validator.fail_fast", "true"));
    when(app.getConfig()).thenReturn(config);

    HibernateValidatorModule module = new HibernateValidatorModule();
    module.install(app);

    verify(services).put(eq(Validator.class), any(Validator.class));
  }

  @Test
  void shouldApplyFluentSettersAndDisableDefaultHandler() throws Exception {
    when(app.getServices()).thenReturn(services);
    when(app.getConfig()).thenReturn(ConfigFactory.empty());

    HibernateValidatorModule module =
        new HibernateValidatorModule()
            .statusCode(StatusCode.BAD_REQUEST)
            .validationTitle("Custom Validation Title")
            .logException()
            .disableViolationHandler(); // This causes the error registration to be skipped

    module.install(app);
  }

  @Test
  void shouldAcceptCustomConstraintValidatorFactories() throws Exception {
    when(app.getServices()).thenReturn(services);
    when(app.getConfig()).thenReturn(ConfigFactory.empty());

    ConstraintValidatorFactory customFactory1 = mock(ConstraintValidatorFactory.class);
    ConstraintValidatorFactory customFactory2 = mock(ConstraintValidatorFactory.class);

    // Chaining hits both `factories == null` and `factories != null` internal list initialization
    HibernateValidatorModule module =
        new HibernateValidatorModule().with(customFactory1).with(customFactory2);

    module.install(app);

    verify(services)
        .put(eq(ConstraintValidatorFactory.class), any(ConstraintValidatorFactory.class));
  }

  @Test
  void beanValidatorImplShouldNotThrowOnEmptyViolations() {
    Validator mockValidator = mock(Validator.class);
    Context mockCtx = mock(Context.class);
    Object testBean = new Object();

    when(mockValidator.validate(testBean)).thenReturn(Collections.emptySet());

    HibernateValidatorModule.BeanValidatorImpl beanValidator =
        new HibernateValidatorModule.BeanValidatorImpl(mockValidator);

    // Assert that no exceptions are thrown when validation succeeds
    assertDoesNotThrow(() -> beanValidator.validate(mockCtx, testBean));
  }

  @Test
  void beanValidatorImplShouldThrowOnViolations() {
    Validator mockValidator = mock(Validator.class);
    Context mockCtx = mock(Context.class);
    Object testBean = new Object();

    ConstraintViolation<Object> mockViolation = mock(ConstraintViolation.class);
    when(mockValidator.validate(testBean)).thenReturn(Set.of(mockViolation));

    HibernateValidatorModule.BeanValidatorImpl beanValidator =
        new HibernateValidatorModule.BeanValidatorImpl(mockValidator);

    // Assert that it bubbles up the ConstraintViolationException
    assertThrows(
        ConstraintViolationException.class, () -> beanValidator.validate(mockCtx, testBean));
  }
}
