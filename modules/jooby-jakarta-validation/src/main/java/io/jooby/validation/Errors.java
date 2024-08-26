package io.jooby.validation;

import java.util.List;

public record Errors(List<String> objectErrors, List<FieldError> fieldErrors) {
}
