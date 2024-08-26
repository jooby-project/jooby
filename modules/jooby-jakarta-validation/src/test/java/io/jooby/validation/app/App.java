package io.jooby.validation.app;

import io.jooby.Jooby;
import io.jooby.Reified;
import io.jooby.StatusCode;
import io.jooby.jackson.JacksonModule;
import io.jooby.validation.BeanValidator;
import io.jooby.validation.ConstraintViolationHandler;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;

import static jakarta.validation.Validation.byProvider;

public class App extends Jooby {

    private static final StatusCode STATUS_CODE = StatusCode.UNPROCESSABLE_ENTITY;
    public static final String DEFAULT_TITLE = "Validation failed";

    {
        provideValidator(this);
        install(new JacksonModule());

        post("/create-person", ctx -> BeanValidator.validate(ctx, ctx.body(Person.class)));

        post("/create-array-of-persons", ctx -> BeanValidator.validate(ctx, ctx.body(Person[].class)));

        post("/create-list-of-persons", ctx -> {
            return BeanValidator.validate(ctx, ctx.body(Reified.list(Person.class).getType()));
        });

        post("/create-map-of-persons", ctx -> {
            return BeanValidator.validate(ctx, ctx.body(Reified.map(String.class, Person.class).getType()));
        });

        post("/create-new-account", ctx -> {
            return BeanValidator.validate(ctx, ctx.body(NewAccountRequest.class));
        });

        error(ConstraintViolationException.class, new ConstraintViolationHandler(STATUS_CODE, DEFAULT_TITLE));
    }

    private void provideValidator(Jooby app) {
        HibernateValidatorConfiguration cfg = byProvider(HibernateValidator.class).configure();
        try (ValidatorFactory factory = cfg.buildValidatorFactory()) {
            Validator validator = factory.getValidator();
            app.getServices().put(Validator.class, validator);
        }
    }
}