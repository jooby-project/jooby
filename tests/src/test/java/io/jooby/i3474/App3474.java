/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3474;

import io.jooby.Jooby;

public class App3474 extends Jooby {
  {
    get(
        "",
        ctx -> {
          var test = ctx.pathMap();
          String id = ctx.path("id").value();
          return id;
        });
  }
}
