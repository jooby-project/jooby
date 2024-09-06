/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.avaje.validator;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.avaje.validation.ConstraintViolationException;
import io.avaje.validation.Validator;
import io.jooby.Context;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.StatusCode;
import io.jooby.validation.MvcValidator;

/**
 * Avaje Validator Module: https://jooby.io/modules/avaje-validator.
 *
 * <pre>{@code
 * {
 *   install(new AvajeValidatorModule());
 *
 * }
 *
 * public class Controller {
 *
 *   @POST("/create")
 *   public void create(@Valid Bean bean) {
 *   }
 *
 * }
 * }</pre>
 *
 * <p>Supports validation of a single bean, list, array, or map.
 *
 * <p>The module also provides a built-in error handler that catches {@link
 * ConstraintViolationException} and transforms it into a {@link
 * io.jooby.validation.ValidationResult}
 *
 * @authors kliushnichenko, SentryMan
 * @since 3.3.1
 */
public class AvajeValidatorModule implements Extension {

  private Consumer<Validator.Builder> configurer;
  private StatusCode statusCode = StatusCode.UNPROCESSABLE_ENTITY;
  private String title = "Validation failed";
  private boolean disableDefaultViolationHandler = false;

  /**
   * Setups a configurer callback.
   *
   * @param configurer Configurer callback.
   * @return This module.
   */
  public AvajeValidatorModule doWith(@NonNull final Consumer<Validator.Builder> configurer) {
    this.configurer = configurer;
    return this;
  }

  /**
   * Overrides the default status code for the errors produced by validation. Default code is
   * UNPROCESSABLE_ENTITY(422)
   *
   * @param statusCode new status code
   * @return This module.
   */
  public AvajeValidatorModule statusCode(@NonNull StatusCode statusCode) {
    this.statusCode = statusCode;
    return this;
  }

  /**
   * Overrides the default title for the errors produced by validation. Default title is "Validation
   * failed"
   *
   * @param title new title
   * @return This module.
   */
  public AvajeValidatorModule validationTitle(@NonNull String title) {
    this.title = title;
    return this;
  }

  /**
   * Disables default constraint violation handler. By default {@link AvajeValidatorModule} provides
   * built-in error handler for the {@link ConstraintViolationException} Such exceptions are
   * transformed into response of {@link io.jooby.validation.ValidationResult} Use this flag to
   * disable default error handler and provide your custom.
   *
   * @return This module.
   */
  public AvajeValidatorModule disableViolationHandler() {
    this.disableDefaultViolationHandler = true;
    return this;
  }

  @Override
  public void install(@NonNull Jooby app) throws Exception {

    var props = app.getEnvironment();

    final var locales = new ArrayList<Locale>();
    final var builder = Validator.builder();
    Optional.ofNullable(props.getProperty("validation.failFast", "false"))
        .map(Boolean::valueOf)
        .ifPresent(builder::failFast);

    Optional.ofNullable(props.getProperty("validation.resourcebundle.names"))
        .map(s -> s.split(","))
        .ifPresent(builder::addResourceBundles);

    Optional.ofNullable(props.getProperty("validation.locale.default"))
        .map(Locale::forLanguageTag)
        .ifPresent(
            l -> {
              builder.setDefaultLocale(l);
              locales.add(l);
            });

    Optional.ofNullable(props.getProperty("validation.locale.addedLocales")).stream()
        .flatMap(s -> Arrays.stream(s.split(",")))
        .map(Locale::forLanguageTag)
        .forEach(
            l -> {
              builder.addLocales(l);
              locales.add(l);
            });

    Optional.ofNullable(props.getProperty("validation.temporal.tolerance.value"))
        .map(Long::valueOf)
        .ifPresent(
            duration -> {
              final var unit =
                  Optional.ofNullable(props.getProperty("validation.temporal.tolerance.chronoUnit"))
                      .map(ChronoUnit::valueOf)
                      .orElse(ChronoUnit.MILLIS);
              builder.temporalTolerance(Duration.of(duration, unit));
            });

    if (configurer != null) {
      configurer.accept(builder);
    }

    Validator validator = builder.build();
    app.getServices().put(Validator.class, validator);
    app.getServices().put(MvcValidator.class, new MvcValidatorImpl(validator));

    if (!disableDefaultViolationHandler) {
      app.error(
          ConstraintViolationException.class, new ConstraintViolationHandler(statusCode, title));
    }
  }

  static class MvcValidatorImpl implements MvcValidator {

    private final Validator validator;

    MvcValidatorImpl(Validator validator) {
      this.validator = validator;
    }

    @Override
    public void validate(Context ctx, Object bean) throws ConstraintViolationException {
      validator.validate(bean, ctx.locale());
    }
  }
}
