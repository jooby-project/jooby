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

import java.lang.reflect.Type;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static jakarta.validation.Validation.byProvider;
import static java.util.Objects.requireNonNull;

public class HbvModule implements Extension {

    private final Predicate<Type> predicate;
    private Consumer<HibernateValidatorConfiguration> configurer;

    public HbvModule() {
        this(none());
    }

    public HbvModule(Predicate<Type> predicate) {
        this.predicate = requireNonNull(predicate, "Predicate is required.");
    }

    public HbvModule(final Class<?>... classes) {
        this.predicate = typeIs(Set.of(classes));
    }

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
            application.messageValidator(validator, predicate);
            application.getServices().put(Validator.class, validator);
        }

    }

    static Predicate<Type> typeIs(final Set<Class<?>> classes) {
        return type -> classes.contains((Class<?>) type);
    }

    static Predicate<Type> none() {
        return type -> false;
    }
}
