/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.problem.data;

import io.jooby.problem.HttpProblem;

class CustomError extends HttpProblem.Error {
  private final String type;

  public CustomError(String detail, String pointer, String type) {
    super(detail, pointer);
    this.type = type;
  }

  public String getType() {
    return type;
  }
}
