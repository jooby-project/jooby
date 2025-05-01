/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples.multiapp;

import io.jooby.Jooby;

public class FooApp extends Jooby {
  {
    var services = getServices();
    services.put(FooService.class, new FooService());
    setContextPath("/foo");
    get(
        "/hello",
        ctx -> {
          return require(FooService.class).hello(ctx.query("name").value("Hello"));
        });

    get(
        "/service",
        ctx -> {
          return require(BarService.class).hello(ctx.query("name").value("Hello"));
        });

    error(
        (ctx, cause, code) -> {
          ctx.send("Foo error handler: " + cause.getMessage());
        });
  }

  public static void main(String[] args) {
    runApp(args, FooApp::new);
  }
}
