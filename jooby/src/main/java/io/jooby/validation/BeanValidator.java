/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.validation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.Map;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.SneakyThrows;
import io.jooby.exception.RegistryException;

/**
 * This class is a helper that provides a single entry point for bean validation without being
 * strictly tied to a specific bean validation implementation. Initially, it is utilized in
 * `jooby-apt` for MVC bean validation, but it can also be used independently in the scripting API.
 * It relies on an implementation of {@link BeanValidator} that should be available in the service
 * registry. For an example implementation, refer to the HibernateValidatorModule.
 *
 * @author kliushnichenko
 * @since 3.3.1
 */
public interface BeanValidator {

  /**
   * Method should validate the bean and throw an exception if any constraint violations are
   * detected
   *
   * @param bean bean to be validated
   * @param ctx request context
   * @throws RuntimeException an exception with violations to be thrown (e.g.
   *     ConstraintViolationException)
   */
  void validate(Context ctx, Object bean) throws RuntimeException;

  /**
   * Validate a value bean. Any non-null value is validated.
   *
   * @param ctx HTTP context
   * @param bean bean instance.
   * @return Bean instance.
   * @param <T> Bean type.
   */
  static <T> T apply(Context ctx, T bean) {
    if (bean != null) {
      BeanValidator validator;
      try {
        validator = ctx.require(BeanValidator.class);
      } catch (RegistryException cause) {
        throw new RegistryException(
            "Unable to load 'BeanValidator' class. Bean validation usage was detected,"
                + " but the appropriate dependency is missing. Please ensure that you have"
                + " added the corresponding validation dependency (e.g.,"
                + " jooby-hibernate-validator, jooby-avaje-validator).");
      }
      if (bean instanceof Iterable<?> values) {
        validateIterable(validator, ctx, values);
      } else if (bean.getClass().isArray()) {
        validateIterable(validator, ctx, List.of((Object[]) bean));
      } else if (bean instanceof Map<?, ?> map) {
        validateIterable(validator, ctx, map.values());
      } else {
        validator.validate(ctx, bean);
      }
    }
    return bean;
  }

  static Route.Filter validate() {
    return BeanValidator::validate;
  }

  static Route.Handler validate(Route.Handler next) {
    return ctx -> {
      try {
        return next.apply(new ValidationContext(ctx));
      } catch (UndeclaredThrowableException | InvocationTargetException e) {
        // unwrap reflective exception
        throw SneakyThrows.propagate(getRootCause(e));
      }
    };
  }

  private static Throwable getRootCause(Throwable throwable) {
    Throwable slowPointer = throwable;

    Throwable cause;
    for (boolean advanceSlowPointer = false;
        (cause = throwable.getCause()) != null;
        advanceSlowPointer = !advanceSlowPointer) {
      throwable = cause;
      if (throwable == slowPointer) {
        throw new IllegalArgumentException("Loop in causal chain detected.", throwable);
      }

      if (advanceSlowPointer) {
        slowPointer = slowPointer.getCause();
      }
    }

    return throwable;
  }

  private static void validateIterable(BeanValidator validator, Context ctx, Iterable<?> beans) {
    for (var item : beans) {
      validator.validate(ctx, item);
    }
  }
}
