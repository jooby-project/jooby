/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3508.data;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class HbvPasswordsShouldMatchValidator
    implements ConstraintValidator<HbvPasswordsShouldMatch, NewAccountRequest> {

  @Override
  public boolean isValid(NewAccountRequest request, ConstraintValidatorContext constraintContext) {
    if (request.getPassword() == null || request.getConfirmPassword() == null) {
      return false;
    }
    return request.getPassword().equals(request.getConfirmPassword());
  }
}
