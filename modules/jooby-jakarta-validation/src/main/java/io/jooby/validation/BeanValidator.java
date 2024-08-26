/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.validation;

import io.jooby.Context;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

import java.util.*;

public final class BeanValidator {

    public static <T> T validate(Context ctx, T bean) {
        Validator validator = ctx.require(Validator.class);

        if (bean instanceof Collection<?>) {
            validateCollection(validator, (Collection<?>) bean);
        } else if (bean.getClass().isArray()) {
            validateCollection(validator, Arrays.asList((Object[])bean));
        } else if (bean instanceof Map<?,?>) {
            validateCollection(validator, ((Map<?,?>)bean).values());
        } else {
            validateObject(validator, bean);
        }

        return bean;
    }

    private static void validateCollection(Validator validator, Collection<?> beans) {
        for (Object item : beans) {
            validateObject(validator, item);
        }
    }

    private static void validateObject(Validator validator, Object bean) {
        Set<ConstraintViolation<Object>> violations = validator.validate(bean);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
