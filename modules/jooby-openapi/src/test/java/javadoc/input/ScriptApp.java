/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package javadoc.input;

import io.jooby.Context;
import io.jooby.Jooby;

/** Script App. Some description. */
public class ScriptApp extends Jooby {
  {
    // Ignored
    /*
     * This is a static path. No parameters
     *
     * @return Request Path.
     */
    get(
        "/static",
        ctx -> {
          // Ignored.
          return ctx.getRequestPath();
          // Ignored
        });

    // Ignored
    /*
     * Path param.
     *
     * @param id Path ID.
     * @return Some value.
     */
    // Ignored
    get("/path/{id}", Context::getRequestPath);
  }
}
