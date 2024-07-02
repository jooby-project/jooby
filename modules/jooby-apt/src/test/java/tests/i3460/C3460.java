/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3460;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import io.jooby.annotation.QueryParam;

@Path("/path")
public class C3460 {
  @GET("/required-string-param")
  public String requiredStringParam(@QueryParam("value") @NonNull String value) {
    return value;
  }
}
