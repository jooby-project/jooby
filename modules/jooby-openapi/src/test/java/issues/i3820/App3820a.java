/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3820;

import io.jooby.Jooby;
import issues.i3820.model.Book;

public class App3820a extends Jooby {
  {
    post(
        "/library/books",
        ctx -> {
          return ctx.body(Book.class);
        });
  }
}
