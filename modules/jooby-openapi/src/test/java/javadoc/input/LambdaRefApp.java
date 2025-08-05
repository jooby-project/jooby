/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package javadoc.input;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Jooby;

public class LambdaRefApp extends Jooby {

  {
    path(
        "/api/pets",
        () -> {
          get("/{id}", this::findPetById);
        });
  }

  /*
   * Find pet by id.
   * @param id Pet ID.
   */
  private @NonNull String findPetById(Context ctx) {
    var id = ctx.path("id").value();
    return "Pets";
  }
}
