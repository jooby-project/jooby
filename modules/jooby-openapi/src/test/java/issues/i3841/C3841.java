/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3841;

import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import io.jooby.annotation.QueryParam;

/** Paths. */
@Path("/3841")
public class C3841 {

  /**
   * Hello endpoint.
   *
   * @param name Name arg.
   * @return Hello endpoint.
   */
  @GET
  public String hello(@QueryParam String name) {
    return null;
  }
}
