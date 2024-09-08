/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.hibernate.validator.app;

import io.jooby.Jooby;
import io.jooby.StatusCode;
import io.jooby.hibernate.validator.HibernateValidatorModule;
import io.jooby.jackson.JacksonModule;

public class App extends Jooby {

  private static final StatusCode STATUS_CODE = StatusCode.UNPROCESSABLE_ENTITY;
  public static final String DEFAULT_TITLE = "Validation failed";

  {
    install(new JacksonModule());
    install(new HibernateValidatorModule().validationTitle(DEFAULT_TITLE).statusCode(STATUS_CODE));

    mvc(new Controller());
  }
}
