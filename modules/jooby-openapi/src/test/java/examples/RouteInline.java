/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.Jooby;

public class RouteInline {

  public static void main(String[] args) {
    Jooby.runApp(
        args,
        app -> {
          app.get("/inline", ctx -> "...");
        });
  }
}
