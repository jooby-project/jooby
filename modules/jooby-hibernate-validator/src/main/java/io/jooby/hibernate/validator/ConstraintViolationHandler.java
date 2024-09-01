package io.jooby.hibernate.validator;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.ErrorHandler;
import io.jooby.StatusCode;
import io.jooby.validation.ValidationResult;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.jooby.validation.ValidationResult.ErrorType.FIELD;
import static io.jooby.validation.ValidationResult.ErrorType.GLOBAL;
import static java.util.stream.Collectors.groupingBy;

/**
 * Catches and transform {@link ConstraintViolationException} into {@link ValidationResult}
 * <p>
 * Payload example:
 * <pre>{@code
 * {
 *    "title": "Validation failed",
 *    "status": 422,
 *    "errors": [
 *       {
 *          "field": null,
 *          "messages": [
 *             "Passwords should match"
 *          ],
 *          "type": "GLOBAL"
 *       },
 *       {
 *          "field": "firstName",
 *          "messages": [
 *             "must not be empty",
 *             "must not be null"
 *          ],
 *          "type": "FIELD"
 *       }
 *    ]
 * }
 * }</pre>
 *
 * @author kliushnichenko
 * @since 3.2.10
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

        List<ValidationResult.Error> errors = collectErrors(groupedByPath);

        ValidationResult result = new ValidationResult(title, statusCode.value(), errors);
        ctx.setResponseCode(statusCode).render(result);
    }

    private List<ValidationResult.Error> collectErrors(Map<String, List<ConstraintViolation<?>>> groupedViolations) {
        List<ValidationResult.Error> errors = new ArrayList<>();
        for (Map.Entry<String, List<ConstraintViolation<?>>> entry : groupedViolations.entrySet()) {
            var path = entry.getKey();
            if (ROOT_VIOLATIONS_PATH.equals(path)) {
                errors.add(new ValidationResult.Error(null, extractMessages(entry.getValue()), GLOBAL));
            } else {
                errors.add(new ValidationResult.Error(path, extractMessages(entry.getValue()), FIELD));
            }
        }
        return errors;
    }

    private List<String> extractMessages(List<ConstraintViolation<?>> violations) {
        return violations.stream().map(ConstraintViolation::getMessage).toList();
    }
}
