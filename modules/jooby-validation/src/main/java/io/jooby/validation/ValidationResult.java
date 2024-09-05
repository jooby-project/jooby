package io.jooby.validation;

import java.util.List;

public record ValidationResult(String title, int status, List<Error> errors) {
    public record Error(String field, List<String> messages, ErrorType type) {
    }

    public enum ErrorType {
        FIELD, GLOBAL
    }
}
