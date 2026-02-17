/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3761;

import io.jooby.annotation.FormParam;
import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import io.jooby.annotation.QueryParam;

@Path("/3761")
public class C3761 {

  @GET("/number")
  public int number(@QueryParam("5") int num) {
    return num;
  }

  @GET("/unset")
  public String unset(@QueryParam String unset) {
    return unset;
  }

  @GET("/emptySet")
  public String emptySet(@QueryParam("") String emptySet) {
    return emptySet;
  }

  @GET("/stringVal")
  public String string(@QueryParam("Hello") String stringVal) {
    return stringVal;
  }

  @GET("/boolVal")
  public boolean bool(@FormParam("false") boolean boolVal) {
    return boolVal;
  }
}
