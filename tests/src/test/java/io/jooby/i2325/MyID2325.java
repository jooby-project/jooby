/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2325;

import jakarta.inject.Inject;

public class MyID2325 {

  private String value;

  @Inject
  public MyID2325(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "MyID:" + value;
  }
}
