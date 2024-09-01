/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.hibernate.validator;

import edu.umd.cs.findbugs.annotations.NonNull;
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

public class HibernateValidatorModule implements Extension {

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
    public void install(@NonNull Jooby app) {
        HibernateValidatorConfiguration cfg = byProvider(HibernateValidator.class).configure();

        if (configurer != null) {
            configurer.accept(cfg);
        }

        try (ValidatorFactory factory = cfg.buildValidatorFactory()) {
            Validator validator = factory.getValidator();
            app.getServices().put(Validator.class, validator);
            app.getServices().put(MvcValidator.class, new MvcValidatorImpl(validator));

            if (!disableDefaultViolationHandler) {
                app.error(ConstraintViolationException.class, new ConstraintViolationHandler(statusCode, title));
            }
        }
    }

    static class MvcValidatorImpl implements MvcValidator<ConstraintViolationException> {

        private final Validator validator;

        MvcValidatorImpl(Validator validator) {
            this.validator = validator;
        }

        @Override
        public void validate(Object bean) {
            Set<ConstraintViolation<Object>> violations = validator.validate(bean);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }
    }
}
