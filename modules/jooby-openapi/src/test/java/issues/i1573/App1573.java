/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i1573;

import io.jooby.Jooby;

public class App1573 extends Jooby {
  {
    get(
        "/profile/{id}?",
        ctx -> {
          return ctx.path("id").value("self");
        });

    mvc(new Controller1573());
  }
}
