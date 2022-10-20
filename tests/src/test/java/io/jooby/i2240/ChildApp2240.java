/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2240;

import io.jooby.Jooby;

public class ChildApp2240 extends Jooby {

  {
    get("/child", ctx -> require(Service2240.class).foo());
  }
}
