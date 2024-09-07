/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.avaje.validator.app;

import io.avaje.validation.adapter.AbstractConstraintAdapter;
import io.avaje.validation.adapter.ConstraintAdapter;
import io.avaje.validation.adapter.ValidationContext.AdapterCreateRequest;

@ConstraintAdapter(PasswordsShouldMatch.class)
public class PasswordsShouldMatchValidator extends AbstractConstraintAdapter<NewAccountRequest> {

  public PasswordsShouldMatchValidator(AdapterCreateRequest request) {
    super(request);
  }

  @Override
  public boolean isValid(NewAccountRequest request) {
    if (request.getPassword() == null || request.getConfirmPassword() == null) {
      return false;
    }
    return request.getPassword().equals(request.getConfirmPassword());
  }
}
