/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.avaje.validator;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueType;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.avaje.validation.ConstraintViolationException;
import io.avaje.validation.Validator;
import io.jooby.Context;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.StatusCode;
import io.jooby.validation.BeanValidator;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

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
 * <p>When ProblemDetails is enabled {@link io.jooby.validation.ValidationResult} transformed
 * to compliant response, see {@link io.jooby.problem.HttpProblem}
 *
 * @author kliushnichenko
 * @author SentryMan
 * @since 3.3.1
 */
public class AvajeValidatorModule implements Extension {
  private Consumer<Validator.Builder> configurer;
  private StatusCode statusCode = StatusCode.UNPROCESSABLE_ENTITY;
  private String title = "Validation failed";
  private boolean disableDefaultViolationHandler = false;
  private boolean logException;

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
   * Ask the error handler to log the exception. Default is: false.
   *
   * @return This module.
   */
  public AvajeValidatorModule logException() {
    this.logException = true;
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
  public void install(@NonNull Jooby app) {
    var conf = app.getConfig();

    final var builder = Validator.builder();
    withProperty(conf, "validation.failFast", conf::getBoolean).ifPresent(builder::failFast);

    withProperty(conf, "validation.resourcebundle.names", path -> getStringList(conf, path))
        .ifPresent(values -> values.forEach(builder::addResourceBundles));
    // Locales from application
    Optional.ofNullable(app.getLocales())
        .ifPresent(
            locales -> {
              builder.setDefaultLocale(locales.get(0));
              locales.stream().skip(1).forEach(builder::addLocales);
            });
    withProperty(conf, "validation.locale.default", conf::getString)
        .map(Locale::forLanguageTag)
        .ifPresent(builder::setDefaultLocale);
    withProperty(conf, "validation.locale.addedLocales", path -> getStringList(conf, path))
        .orElseGet(List::of)
        .stream()
        .map(Locale::forLanguageTag)
        .forEach(builder::addLocales);
    withProperty(conf, "validation.temporal.tolerance.value", conf::getLong)
        .ifPresent(
            duration -> {
              var unit =
                  withProperty(conf, "validation.temporal.tolerance.chronoUnit", conf::getString)
                      .map(ChronoUnit::valueOf)
                      .orElse(ChronoUnit.MILLIS);
              builder.temporalTolerance(Duration.of(duration, unit));
            });

    if (configurer != null) {
      configurer.accept(builder);
    }

    var validator = builder.build();
    app.getServices().put(Validator.class, validator);
    app.getServices().put(BeanValidator.class, new BeanValidatorImpl(validator));

    if (!disableDefaultViolationHandler) {
      app.error(new ConstraintViolationHandler(
          statusCode, title, logException, app.problemDetailsIsEnabled())
      );
    }
  }

  static class BeanValidatorImpl implements BeanValidator {

    private final Validator validator;

    BeanValidatorImpl(Validator validator) {
      this.validator = validator;
    }

    @Override
    public void validate(Context ctx, Object bean) throws ConstraintViolationException {
      validator.validate(bean, ctx.locale());
    }
  }

  private static <T> Optional<T> withProperty(Config config, String path, Function<String, T> fn) {
    if (config.hasPath(path)) {
      return Optional.of(fn.apply(path));
    }
    return Optional.empty();
  }

  private static List<String> getStringList(Config config, String path) {
    return config.getValue(path).valueType() == ConfigValueType.STRING
        ? List.of(config.getString(path))
        : config.getStringList(path);
  }
}
