/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.Jooby;

public class FilterApp extends Jooby {

  {
    get("/", ctx -> "Welcome");

    get("/profile", ctx -> "Profile");

    path(
        "/api",
        () -> {
          get("/profile", ctx -> "profile");
        });

    get("/api/profile/{id}", ctx -> ctx.path("id").value());
  }
}
