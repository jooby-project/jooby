/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.hibernate.validator;

import com.typesafe.config.Config;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.StatusCode;
import io.jooby.validation.MvcValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;

import java.util.Set;
import java.util.function.Consumer;

import static jakarta.validation.Validation.byProvider;

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
 * <p>Supports validation of a single bean, list, array, or map.</p>
 *
 * <p>The module also provides a built-in error handler that catches {@link ConstraintViolationException}
 * and transforms it into a {@link io.jooby.validation.ValidationResult}</p>
 *
 * @author kliushnichenko
 * @since 3.2.10
 */
public class HibernateValidatorModule implements Extension {

    private static final String CONFIG_ROOT_PATH = "hibernate.validator";

    private Consumer<HibernateValidatorConfiguration> configurer;
    private StatusCode statusCode = StatusCode.UNPROCESSABLE_ENTITY;
    private String title = "Validation failed";
    private boolean disableDefaultViolationHandler = false;

    /**
     * Setups a configurer callback.
     *
     * @param configurer Configurer callback.
     * @return This module.
     */
    public HibernateValidatorModule doWith(@NonNull final Consumer<HibernateValidatorConfiguration> configurer) {
        this.configurer = configurer;
        return this;
    }

    /**
     * Overrides the default status code for the errors produced by validation.
     * Default code is UNPROCESSABLE_ENTITY(422)
     *
     * @param statusCode new status code
     * @return This module.
     */
    public HibernateValidatorModule statusCode(@NonNull StatusCode statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    /**
     * Overrides the default title for the errors produced by validation.
     * Default title is "Validation failed"
     *
     * @param title new title
     * @return This module.
     */
    public HibernateValidatorModule validationTitle(@NonNull String title) {
        this.title = title;
        return this;
    }

    /**
     * Disables default constraint violation handler.
     * By default {@link HibernateValidatorModule} provides
     * built-in error handler for the {@link ConstraintViolationException}
     * Such exceptions are transformed into response of {@link io.jooby.validation.ValidationResult}
     * Use this flag to disable default error handler and provide your custom.
     *
     * @return This module.
     */
    public HibernateValidatorModule disableViolationHandler() {
        this.disableDefaultViolationHandler = true;
        return this;
    }

    @Override
    public void install(@NonNull Jooby app) throws Exception {
        Config config = app.getConfig();
        HibernateValidatorConfiguration hbvConfig = byProvider(HibernateValidator.class).configure();

        if (config.hasPath(CONFIG_ROOT_PATH)) {
            config.getConfig(CONFIG_ROOT_PATH)
                    .root()
                    .forEach((k, v) -> hbvConfig.addProperty(CONFIG_ROOT_PATH + "." + k, v.unwrapped().toString()));
        }

        if (configurer != null) {
            configurer.accept(hbvConfig);
        }

        try (ValidatorFactory factory = hbvConfig.buildValidatorFactory()) {
            Validator validator = factory.getValidator();
            app.getServices().put(Validator.class, validator);
            app.getServices().put(MvcValidator.class, new MvcValidatorImpl(validator));

            if (!disableDefaultViolationHandler) {
                app.error(ConstraintViolationException.class, new ConstraintViolationHandler(statusCode, title));
            }
        }
    }

    static class MvcValidatorImpl implements MvcValidator {

        private final Validator validator;

        MvcValidatorImpl(Validator validator) {
            this.validator = validator;
        }

        @Override
        public void validate(Context ctx, Object bean) throws ConstraintViolationException {
            Set<ConstraintViolation<Object>> violations = validator.validate(bean);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }
    }
}
