/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i2405;

public class Bean2405 {

  private String value;

  public Bean2405(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }
}
