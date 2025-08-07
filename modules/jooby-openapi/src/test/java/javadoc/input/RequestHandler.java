/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package javadoc.input;

import io.jooby.Context;

public class RequestHandler {

  /*
   * External doc.
   */
  public static String external(Context ctx) {
    return ctx.path("external").value();
  }
}
