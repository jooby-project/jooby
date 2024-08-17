/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.hbv;


import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Extension;
import io.jooby.Jooby;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;

import java.util.function.Consumer;

import static jakarta.validation.Validation.byProvider;
import static java.util.Objects.requireNonNull;

public class HbvModule implements Extension {

    private Consumer<HibernateValidatorConfiguration> configurer;

    public HbvModule doWith(final Consumer<HibernateValidatorConfiguration> configurer) {
        this.configurer = requireNonNull(configurer, "Configurer callback is required.");
        return this;
    }

    @Override
    public void install(@NonNull Jooby application) {
        HibernateValidatorConfiguration cfg = byProvider(HibernateValidator.class).configure();

        if (configurer != null) {
            configurer.accept(cfg);
        }

        try (ValidatorFactory factory = cfg.buildValidatorFactory()) {
            Validator validator = factory.getValidator();
            application.getServices().put(Validator.class, validator);
        }

    }
}
