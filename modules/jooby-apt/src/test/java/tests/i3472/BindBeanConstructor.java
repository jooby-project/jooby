/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3472;

import io.jooby.Context;

public class BindBeanConstructor {

  private final String value;

  public BindBeanConstructor(Context ctx) {
    this.value = "bean-constructor:" + ctx.query("value").value();
  }

  @Override
  public String toString() {
    return value;
  }
}
