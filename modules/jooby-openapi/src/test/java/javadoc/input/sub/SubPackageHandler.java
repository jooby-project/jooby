/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package javadoc.input.sub;

import io.jooby.Context;

public class SubPackageHandler {

  /*
   * Sub package doc.
   *
   * @param external External parameter.
   */
  public static String subPackage(Context ctx) {
    return ctx.path("external").value();
  }
}
