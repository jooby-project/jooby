/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package source;

import io.jooby.Context;
import io.jooby.annotation.GET;
import io.jooby.annotation.POST;
import io.jooby.annotation.Path;

@Path("/inherited")
public class Controller1552 extends Controller1552Base {
  @GET(path = "/childOnly")
  public String childOnly(Context ctx) {
    return ctx.getRequestPath();
  }

  @POST(path = "/childOnly")
  public String childOnly_Post(Context ctx) {
    return ctx.getRequestPath();
  }
}
