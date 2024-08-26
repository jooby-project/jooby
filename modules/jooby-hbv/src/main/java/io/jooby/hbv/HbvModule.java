/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.hbv;


import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.StatusCode;
import io.jooby.validation.ConstraintViolationHandler;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;

import java.util.function.Consumer;

import static jakarta.validation.Validation.byProvider;

public class HbvModule implements Extension {

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
    public HbvModule doWith(@NonNull final Consumer<HibernateValidatorConfiguration> configurer) {
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
    public HbvModule statusCode(@NonNull StatusCode statusCode) {
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
    public HbvModule validationTitle(@NonNull String title) {
        this.title = title;
        return this;
    }

    /**
     * Disables default constraint violation handler.
     * By default {@link HbvModule} provide built-in error handler for the {@link ConstraintViolationException}
     * Such exceptions are transformed into response of {@link io.jooby.validation.ValidationResult}
     * Use this flag to disable default error handler and provide your custom.
     *
     * @return This module.
     */
    public HbvModule disableViolationHandler() {
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

            if (!disableDefaultViolationHandler) {
                app.error(ConstraintViolationException.class, new ConstraintViolationHandler(statusCode, title));
            }
        }
    }
}
