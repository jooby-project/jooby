/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i2525;

import java.util.Optional;

public class MyID2525 {

  private String value;

  public MyID2525(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "MyID:" + Optional.ofNullable(value).orElse("{}");
  }
}
