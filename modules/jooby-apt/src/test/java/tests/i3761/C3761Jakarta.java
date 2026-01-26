/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3761;

import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

@Path("/3761")
public class C3761Jakarta {

  @GET("/number")
  public int number(@QueryParam("num") @DefaultValue("5") int num) {
    return num;
  }

  @GET("/unset")
  public String unset(@QueryParam("unset") String unset) {
    return unset;
  }

  @GET("/emptySet")
  public String emptySet(@QueryParam("emptySet") @DefaultValue("") String emptySet) {
    return emptySet;
  }

  @GET("/stringVal")
  public String string(@QueryParam("stringVal") @DefaultValue("Hello") String stringVal) {
    return stringVal;
  }
}
