/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3472;

import io.jooby.Context;

public record BindBean(String value) {

  public static BindBean of(Context ctx) {
    return new BindBean("bean-factory-method:" + ctx.query("value").value());
  }
}
