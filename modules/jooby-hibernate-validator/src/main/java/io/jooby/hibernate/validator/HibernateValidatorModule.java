/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.hibernate.validator;

import static jakarta.validation.Validation.byProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.*;
import io.jooby.internal.hibernate.validator.CompositeConstraintValidatorFactory;
import io.jooby.validation.BeanValidator;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

/**
 * Hibernate Validator Module: https://jooby.io/modules/hibernate-validator.
 *
 * <pre>{@code
 * {
 *   install(new HibernateValidatorModule());
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
 * io.jooby.validation.ValidationResult}.
 *
 * <p>When ProblemDetails is enabled {@link io.jooby.validation.ValidationResult} transformed to
 * compliant response, see {@link io.jooby.problem.HttpProblem}
 *
 * @author kliushnichenko
 * @since 3.3.1
 */
public class HibernateValidatorModule implements Extension {
  private static final String CONFIG_ROOT_PATH = "hibernate.validator";
  // TODO: remove it on next major
  private Consumer<HibernateValidatorConfiguration> configurer;
  private StatusCode statusCode = StatusCode.UNPROCESSABLE_ENTITY;
  private String title = "Validation failed";
  private boolean disableDefaultViolationHandler = false;
  private boolean logException = false;
  private List<ConstraintValidatorFactory> factories;
  private final HibernateValidatorConfiguration configuration;

  public HibernateValidatorModule(@NonNull HibernateValidatorConfiguration configuration) {
    this.configuration = configuration;
  }

  public HibernateValidatorModule() {
    this(byProvider(HibernateValidator.class).configure());
  }

  /**
   * Setups a configurer callback.
   *
   * @param configurer Configurer callback.
   * @return This module.
   * @deprecated Use {@link
   *     HibernateValidatorModule#HibernateValidatorModule(HibernateValidatorConfiguration)}
   */
  @Deprecated
  public HibernateValidatorModule doWith(
      @NonNull final Consumer<HibernateValidatorConfiguration> configurer) {
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
  public HibernateValidatorModule statusCode(@NonNull StatusCode statusCode) {
    this.statusCode = statusCode;
    return this;
  }

  /**
   * Ask the error handler to log the exception. Default is: false.
   *
   * @return This module.
   */
  public HibernateValidatorModule logException() {
    this.logException = true;
    return this;
  }

  /**
   * Overrides the default title for the errors produced by validation. Default title is "Validation
   * failed"
   *
   * @param title new title
   * @return This module.
   */
  public HibernateValidatorModule validationTitle(@NonNull String title) {
    this.title = title;
    return this;
  }

  /**
   * Disables default constraint violation handler. By default {@link HibernateValidatorModule}
   * provides built-in error handler for the {@link ConstraintViolationException} Such exceptions
   * are transformed into response of {@link io.jooby.validation.ValidationResult} Use this flag to
   * disable default error handler and provide your custom.
   *
   * @return This module.
   */
  public HibernateValidatorModule disableViolationHandler() {
    this.disableDefaultViolationHandler = true;
    return this;
  }

  /**
   * Add a custom {@link ConstraintValidatorFactory}. This factory is allowed to returns <code>null
   * </code> allowing next factory to create an instance (default or one provided by DI).
   *
   * @param factory Factory.
   * @return This module.
   */
  public HibernateValidatorModule with(ConstraintValidatorFactory factory) {
    if (factories == null) {
      factories = new ArrayList<>();
    }
    this.factories.add(factory);
    return this;
  }

  @Override
  public void install(@NonNull Jooby app) throws Exception {
    var config = app.getConfig();
    if (config.hasPath(CONFIG_ROOT_PATH)) {
      config
          .getConfig(CONFIG_ROOT_PATH)
          .root()
          .forEach(
              (k, v) ->
                  configuration.addProperty(CONFIG_ROOT_PATH + "." + k, v.unwrapped().toString()));
    }

    // Set default constraint validator factory.
    var delegateFactory =
        new CompositeConstraintValidatorFactory(
            app, configuration.getDefaultConstraintValidatorFactory());
    if (this.factories != null) {
      this.factories.forEach(delegateFactory::add);
      this.factories.clear();
    }
    configuration.constraintValidatorFactory(delegateFactory);
    if (configurer != null) {
      configurer.accept(configuration);
    }
    var services = app.getServices();
    try (var factory = configuration.buildValidatorFactory()) {
      var validator = factory.getValidator();
      services.put(Validator.class, validator);
      services.put(BeanValidator.class, new BeanValidatorImpl(validator));
      // Allow to access validator factory so hibernate can access later
      var constraintValidatorFactory = factory.getConstraintValidatorFactory();
      services.put(ConstraintValidatorFactory.class, constraintValidatorFactory);

      if (!disableDefaultViolationHandler) {
        app.error(
            ConstraintViolationException.class,
            new ConstraintViolationHandler(
                statusCode, title, logException, app.problemDetailsIsEnabled()));
      }
    }
  }

  static class BeanValidatorImpl implements BeanValidator {

    private final Validator validator;

    BeanValidatorImpl(Validator validator) {
      this.validator = validator;
    }

    @Override
    public void validate(Context ctx, Object bean) throws ConstraintViolationException {
      var violations = validator.validate(bean);
      if (!violations.isEmpty()) {
        throw new ConstraintViolationException(violations);
      }
    }
  }
}
