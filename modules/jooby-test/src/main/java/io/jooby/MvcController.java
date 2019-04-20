package io.jooby;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;

@Path("/mvc")
public class MvcController {

  @GET
  public String getIt(Context ctx) {
    return ctx.pathString();
  }
}
