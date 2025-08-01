/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package javadoc.input;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import io.jooby.annotation.QueryParam;

@Path("/api")
public class NoClassDoc {

  /**
   * Method Doc.
   *
   * @param name Name.
   * @return Person name.
   */
  @NonNull @GET
  public String hello(@QueryParam String name) {
    return "hello";
  }
}
