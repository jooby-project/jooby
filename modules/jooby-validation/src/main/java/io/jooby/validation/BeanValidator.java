/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.validation;

import io.jooby.Context;
import io.jooby.SneakyThrows;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public final class BeanValidator {

    public static <T> T validate(Context ctx, T bean) {
        MvcValidator<?> validator = ctx.require(MvcValidator.class);

        if (bean instanceof Collection<?>) {
            validateCollection(validator, (Collection<?>) bean);
        } else if (bean.getClass().isArray()) {
            validateCollection(validator, Arrays.asList((Object[]) bean));
        } else if (bean instanceof Map<?, ?>) {
            validateCollection(validator, ((Map<?, ?>) bean).values());
        } else {
            validateObject(validator, bean);
        }

        return bean;
    }

    private static void validateCollection(MvcValidator<?> validator, Collection<?> beans) {
        for (Object item : beans) {
            validateObject(validator, item);
        }
    }

    private static void validateObject(MvcValidator<?> validator, Object bean) {
        try {
            validator.validate(bean);
        } catch (Throwable e) {
            SneakyThrows.propagate(e);
        }
    }
}
