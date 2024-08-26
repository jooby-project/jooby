package io.jooby.validation;

import java.util.List;

public record FieldError(String field, List<String> messages) {
}
