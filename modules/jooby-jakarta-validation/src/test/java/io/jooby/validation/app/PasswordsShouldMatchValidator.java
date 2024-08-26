package io.jooby.validation.app;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordsShouldMatchValidator implements ConstraintValidator<PasswordsShouldMatch, NewAccountRequest> {

    @Override
    public boolean isValid(NewAccountRequest request, ConstraintValidatorContext constraintContext) {
        if (request.getPassword() == null || request.getConfirmPassword() == null) {
            return false;
        }
        return request.getPassword().equals(request.getConfirmPassword());
    }
}
