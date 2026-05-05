/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.avaje.validator;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import io.avaje.validation.Validator;
import io.jooby.Context;
import io.jooby.ErrorHandler;
import io.jooby.Jooby;
import io.jooby.ServiceRegistry;
import io.jooby.StatusCode;
import io.jooby.internal.avaje.validator.ConstraintViolationMapper;
import io.jooby.validation.BeanValidator;
import io.jooby.validation.ValidationExceptionMapper;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class AvajeValidatorModuleTest {

  @Mock Jooby app;
  @Mock ServiceRegistry registry;
  @Mock Config config;

  @BeforeEach
  void setup() {
    lenient().when(app.getConfig()).thenReturn(config);
    lenient().when(app.getServices()).thenReturn(registry);
    lenient().when(app.problemDetailsIsEnabled()).thenReturn(false);

    lenient().when(app.getLocales()).thenReturn(null);
  }

  @Test
  void testInstall_Defaults() {
    when(config.hasPath(anyString())).thenReturn(false);

    ServiceRegistry.MultiBinder<ValidationExceptionMapper> exceptionMappers =
        mock(ServiceRegistry.MultiBinder.class);
    when(registry.listOf(ValidationExceptionMapper.class)).thenReturn(exceptionMappers);

    try (MockedStatic<Validator> validatorMock = mockStatic(Validator.class)) {
      Validator.Builder builder = mock(Validator.Builder.class, Answers.RETURNS_SELF);
      Validator validator = mock(Validator.class);
      validatorMock.when(Validator::builder).thenReturn(builder);
      when(builder.build()).thenReturn(validator);

      AvajeValidatorModule module = new AvajeValidatorModule();
      module.install(app);

      verify(registry).put(Validator.class, validator);
      ArgumentCaptor<BeanValidator> beanValidatorCaptor =
          ArgumentCaptor.forClass(BeanValidator.class);
      verify(registry).put(eq(BeanValidator.class), beanValidatorCaptor.capture());
      assertNotNull(beanValidatorCaptor.getValue());

      verify(exceptionMappers).add(any(ConstraintViolationMapper.class));

      verify(app).error(any(ConstraintViolationHandler.class));
    }
  }

  @Test
  void testInstall_WithConfigStringsAndLocales() {
    when(config.hasPath(anyString())).thenReturn(true);

    when(config.getBoolean("validation.failFast")).thenReturn(true);

    ConfigValue rbNameVal = mock(ConfigValue.class);
    when(rbNameVal.valueType()).thenReturn(ConfigValueType.STRING);
    when(config.getValue("validation.resourcebundle.names")).thenReturn(rbNameVal);
    when(config.getString("validation.resourcebundle.names")).thenReturn("messages");

    when(app.getLocales()).thenReturn(List.of(Locale.US, Locale.UK));

    when(config.getString("validation.locale.default")).thenReturn("fr-FR");

    ConfigValue addedLocalesVal = mock(ConfigValue.class);
    when(addedLocalesVal.valueType()).thenReturn(ConfigValueType.STRING);
    when(config.getValue("validation.locale.addedLocales")).thenReturn(addedLocalesVal);
    when(config.getString("validation.locale.addedLocales")).thenReturn("de-DE");

    when(config.getLong("validation.temporal.tolerance.value")).thenReturn(100L);
    when(config.getString("validation.temporal.tolerance.chronoUnit")).thenReturn("SECONDS");

    ServiceRegistry.MultiBinder<ValidationExceptionMapper> exceptionMappers =
        mock(ServiceRegistry.MultiBinder.class);
    when(registry.listOf(ValidationExceptionMapper.class)).thenReturn(exceptionMappers);

    try (MockedStatic<Validator> validatorMock = mockStatic(Validator.class)) {
      Validator.Builder builder = mock(Validator.Builder.class, Answers.RETURNS_SELF);
      Validator validator = mock(Validator.class);
      validatorMock.when(Validator::builder).thenReturn(builder);
      when(builder.build()).thenReturn(validator);

      new AvajeValidatorModule().install(app);

      verify(builder).failFast(true);
      verify(builder).addResourceBundles("messages");

      verify(builder).setDefaultLocale(Locale.US);
      verify(builder).addLocales(Locale.UK);

      verify(builder).setDefaultLocale(Locale.forLanguageTag("fr-FR"));
      verify(builder).addLocales(Locale.forLanguageTag("de-DE"));

      verify(builder).temporalTolerance(Duration.of(100, ChronoUnit.SECONDS));
    }
  }

  @Test
  void testInstall_WithConfigListsAndDefaultChronoUnit() {
    when(config.hasPath(anyString())).thenReturn(false);
    when(config.hasPath("validation.resourcebundle.names")).thenReturn(true);
    when(config.hasPath("validation.locale.addedLocales")).thenReturn(true);
    when(config.hasPath("validation.temporal.tolerance.value")).thenReturn(true);

    ConfigValue rbNameVal = mock(ConfigValue.class);
    when(rbNameVal.valueType()).thenReturn(ConfigValueType.LIST);
    when(config.getValue("validation.resourcebundle.names")).thenReturn(rbNameVal);
    when(config.getStringList("validation.resourcebundle.names"))
        .thenReturn(List.of("msg1", "msg2"));

    ConfigValue addedLocalesVal = mock(ConfigValue.class);
    when(addedLocalesVal.valueType()).thenReturn(ConfigValueType.LIST);
    when(config.getValue("validation.locale.addedLocales")).thenReturn(addedLocalesVal);
    when(config.getStringList("validation.locale.addedLocales")).thenReturn(List.of("es", "it"));

    when(config.getLong("validation.temporal.tolerance.value")).thenReturn(50L);

    ServiceRegistry.MultiBinder<ValidationExceptionMapper> exceptionMappers =
        mock(ServiceRegistry.MultiBinder.class);
    when(registry.listOf(ValidationExceptionMapper.class)).thenReturn(exceptionMappers);

    try (MockedStatic<Validator> validatorMock = mockStatic(Validator.class)) {
      Validator.Builder builder = mock(Validator.Builder.class, Answers.RETURNS_SELF);
      Validator validator = mock(Validator.class);
      validatorMock.when(Validator::builder).thenReturn(builder);
      when(builder.build()).thenReturn(validator);

      new AvajeValidatorModule().install(app);

      verify(builder).addResourceBundles("msg1");
      verify(builder).addResourceBundles("msg2");
      verify(builder).addLocales(Locale.forLanguageTag("es"));
      verify(builder).addLocales(Locale.forLanguageTag("it"));

      verify(builder).temporalTolerance(Duration.of(50, ChronoUnit.MILLIS));
    }
  }

  @Test
  void testInstall_WithModuleBuilderMethodsAndDisabledHandler() {
    when(config.hasPath(anyString())).thenReturn(false);

    ServiceRegistry.MultiBinder<ValidationExceptionMapper> exceptionMappers =
        mock(ServiceRegistry.MultiBinder.class);
    when(registry.listOf(ValidationExceptionMapper.class)).thenReturn(exceptionMappers);

    try (MockedStatic<Validator> validatorMock = mockStatic(Validator.class)) {
      Validator.Builder builder = mock(Validator.Builder.class, Answers.RETURNS_SELF);
      Validator validator = mock(Validator.class);
      validatorMock.when(Validator::builder).thenReturn(builder);
      when(builder.build()).thenReturn(validator);

      AvajeValidatorModule module =
          new AvajeValidatorModule()
              .doWith(b -> b.failFast(false))
              .statusCode(StatusCode.BAD_REQUEST)
              .validationTitle("Custom Validation Title")
              .logException()
              .disableViolationHandler();

      module.install(app);

      verify(builder).failFast(false);

      verify(app, never()).error(any(ErrorHandler.class));
    }
  }

  @Test
  void testBeanValidatorImpl_Validate() {
    Validator validator = mock(Validator.class);
    Context ctx = mock(Context.class);
    when(ctx.locale()).thenReturn(Locale.CANADA);

    AvajeValidatorModule.BeanValidatorImpl beanValidator =
        new AvajeValidatorModule.BeanValidatorImpl(validator);

    Object testBean = new Object();
    beanValidator.validate(ctx, testBean);

    verify(validator).validate(testBean, Locale.CANADA);
  }
}
