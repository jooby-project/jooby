package io.jooby.validation;

import io.jooby.Context;

/**
 * This interface should be implemented by modules that provide bean validation functionality.
 * An instance of this interface must be registered in the Jooby service registry.
 * Doing so will enable bean validation for MVC routes.
 * For an example implementation, refer to the HibernateValidatorModule.
 */
public interface MvcValidator {

    /**
     * Method should validate the bean and throw an exception if any constraint violations are detected
     * @param bean bean to be validated
     * @param ctx request context
     * @throws RuntimeException an exception with violations to be thrown (e.g. ConstraintViolationException)
     */
    void validate(Context ctx, Object bean) throws RuntimeException;
}
