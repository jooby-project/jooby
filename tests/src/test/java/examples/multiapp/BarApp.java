/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples.multiapp;

import io.jooby.Jooby;

public class BarApp extends Jooby {
  {
    var services = getServices();
    services.put(BarService.class, new BarService());
    setContextPath("/bar");
    get(
        "/hello",
        ctx -> {
          return require(BarService.class).hello(ctx.query("name").value("Hello"));
        });

    get(
        "/service",
        ctx -> {
          return require(FooService.class).hello(ctx.query("name").value("Hello"));
        });
  }
}
