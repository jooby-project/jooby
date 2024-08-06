package io.jooby;

import jakarta.validation.Validator;

import java.lang.reflect.Type;
import java.util.function.Predicate;

public record MessageValidator(Validator validator, Predicate<Type> predicate) {
}
