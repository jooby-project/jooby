/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.hibernate.validator.app;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordsShouldMatchValidator
    implements ConstraintValidator<PasswordsShouldMatch, NewAccountRequest> {

  @Override
  public boolean isValid(NewAccountRequest request, ConstraintValidatorContext constraintContext) {
    if (request.getPassword() == null || request.getConfirmPassword() == null) {
      return false;
    }
    return request.getPassword().equals(request.getConfirmPassword());
  }
}
