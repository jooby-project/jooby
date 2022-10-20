/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.Context;

public class ExternalReference {

  public String routeReference(Context ctx) {
    return ctx.toString();
  }

  public static String externalStaticReference(Context ctx) {
    return ctx.toString();
  }
}
