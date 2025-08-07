/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package javadoc.input;

import javadoc.input.sub.SubPackageHandler;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Jooby;

/**
 * Lambda App.
 *
 * <p>Using method ref.
 */
public class LambdaRefApp extends Jooby {
  {
    get("/reference", this::findPetById);
    post("/static-reference", javadoc.input.LambdaRefApp::staticFindPetById);
    put("/external-reference", RequestHandler::external);
    get("/external-subPackage-reference", SubPackageHandler::subPackage);
  }

  /*
   * Find pet by id.
   *
   * @param id Pet ID.
   */
  private @NonNull String findPetById(Context ctx) {
    var id = ctx.path("id").value();
    return "Pets";
  }

  /*
   Static reference.

   Description in next line.
   @param id Path ID.
  */
  private static @NonNull String staticFindPetById(Context ctx) {
    var id = ctx.path("id").value();
    return "Pets";
  }
}
