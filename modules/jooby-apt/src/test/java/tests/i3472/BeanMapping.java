/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3472;

import io.jooby.Context;

public class BeanMapping {
  public static BindBean map(Context ctx) {
    return new BindBean("extends:" + ctx.query("value").value());
  }
}
