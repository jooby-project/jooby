/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.problem.data;

class MyCustomException extends RuntimeException {
  public MyCustomException(String message) {
    super(message);
  }
}
