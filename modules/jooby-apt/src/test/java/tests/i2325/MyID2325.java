/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i2325;

import java.util.Optional;

public class MyID2325 {

  private String value;

  public MyID2325(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "MyID:" + Optional.ofNullable(value).orElse("{}");
  }
}
