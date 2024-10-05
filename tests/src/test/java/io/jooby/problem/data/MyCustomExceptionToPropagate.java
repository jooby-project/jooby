/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.problem.data;

class MyCustomExceptionToPropagate extends RuntimeException {
  public MyCustomExceptionToPropagate(String message) {
    super(message);
  }
}
