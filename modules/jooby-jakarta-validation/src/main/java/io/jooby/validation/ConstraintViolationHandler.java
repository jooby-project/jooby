package io.jooby.validation;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.ErrorHandler;
import io.jooby.StatusCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.groupingBy;

/**
 * Catches and transform {@link ConstraintViolationException} into {@link ValidationResult}
 *
 * Payload example:
 * <code>
 * {
 *     "title": "Validation failed",
 *     "status": 422,
 *     "errors": {
 *         "objectErrors": [
 *              "Passwords should match"
 *         ],
 *         "fieldErrors": [
 *             {
 *                 "field": "firstName",
 *                 "messages": [
 *                     "must not be empty",
 *                     "must not be null"
 *                 ]
 *             }
 *         ]
 *     }
 * }
 * </code>
 */
public class ConstraintViolationHandler implements ErrorHandler {

    private static final String ROOT_VIOLATIONS_PATH = "";

    private final StatusCode statusCode;
    private final String title;

    public ConstraintViolationHandler(StatusCode statusCode, String title) {
        this.statusCode = statusCode;
        this.title = title;
    }

    @Override
    public void apply(@NonNull Context ctx, @NonNull Throwable cause, @NonNull StatusCode code) {
        ConstraintViolationException ex = (ConstraintViolationException) cause;

        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();

        Map<String, List<ConstraintViolation<?>>> groupedByPath = violations.stream()
                .collect(groupingBy(violation -> violation.getPropertyPath().toString()));

        List<FieldError> fieldErrors = collectFieldErrors(groupedByPath);
        List<String> objectErrors = collectObjectErrors(groupedByPath);

        Errors errors = new Errors(objectErrors, fieldErrors);
        ValidationResult result = new ValidationResult(title, statusCode.value(), errors);
        ctx.setResponseCode(statusCode).render(result);
    }

    private List<FieldError> collectFieldErrors(Map<String, List<ConstraintViolation<?>>> groupedViolations) {
        List<FieldError> fieldErrors = new ArrayList<>();
        for (Map.Entry<String, List<ConstraintViolation<?>>> entry : groupedViolations.entrySet()) {
            var field = entry.getKey();
            if (!ROOT_VIOLATIONS_PATH.equals(field)) {
                fieldErrors.add(new FieldError(field, extractMessages(entry.getValue())));
            }
        }
        return fieldErrors;
    }

    private List<String> collectObjectErrors(Map<String, List<ConstraintViolation<?>>> groupedViolations) {
        List<ConstraintViolation<?>> violations = groupedViolations.get(ROOT_VIOLATIONS_PATH);
        if (violations != null) {
            return extractMessages(violations);
        }
        return List.of();
    }

    private List<String> extractMessages(List<ConstraintViolation<?>> violations) {
        return violations.stream().map(ConstraintViolation::getMessage).toList();
    }
}
