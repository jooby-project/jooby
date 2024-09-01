package io.jooby.validation;

public interface MvcValidator<E extends Throwable> {

    void validate(Object bean) throws E;
}
