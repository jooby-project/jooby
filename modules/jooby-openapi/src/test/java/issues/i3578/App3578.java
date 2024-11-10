/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3578;

import io.jooby.Context;
import io.jooby.Jooby;

public class App3578 extends Jooby {
  {
    path(
        "/api/pets",
        () -> {
          get("/", Context::getRequestPath);
          get("/{id}", Context::getRequestPath);
          post("/", Context::getRequestPath);
          put("/{id}", Context::getRequestPath);
          delete("/{id}", Context::getRequestPath);
        });
  }
}
