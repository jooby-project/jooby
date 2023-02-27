/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i1807;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.annotation.FormParam;
import io.jooby.annotation.POST;
import io.jooby.annotation.Path;

public class C1807 {
  @Path("/test/{word}")
  @POST
  public Word1807 hello(@FormParam @NonNull Word1807 data) {
    return data;
  }
}
