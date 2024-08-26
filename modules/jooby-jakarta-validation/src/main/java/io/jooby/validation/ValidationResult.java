package io.jooby.validation;

public record ValidationResult(String title, int status, Errors errors) {
}
