/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.Jooby;

public class FormApp extends Jooby {

  {
    post(
        "/single",
        ctx -> {
          ctx.form("name").value();
          return "...";
        });

    post(
        "/multiple",
        ctx -> {
          ctx.form("firstname").value();
          ctx.form("lastname").value();
          ctx.file("picture");
          return "...";
        });

    post(
        "/bean",
        ctx -> {
          AForm form = ctx.form(AForm.class);
          return "...";
        });
  }
}
