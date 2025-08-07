/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package javadoc.input;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Jooby;

public class MultilineComment extends Jooby {
  {
    get("/multiline", this::multilineComment);
  }

  /*
   Multiline comment.

   Description in next
   line.
   @param id Path ID.
  */
  private @NonNull String multilineComment(Context ctx) {
    var id = ctx.path("id").value();
    return "Pets";
  }
}
