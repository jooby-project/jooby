package org.jooby.js;

import org.jooby.mvc.GET;
import org.jooby.mvc.Path;

@Path("/xap")
public class Pets {

  @GET
  public String get() {
    return  "xx";
  }
}
