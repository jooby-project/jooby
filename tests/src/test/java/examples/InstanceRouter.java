/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.slf4j.LoggerFactory;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.annotation.DELETE;
import io.jooby.annotation.GET;
import io.jooby.annotation.POST;
import io.jooby.annotation.Path;

@Path("/")
public class InstanceRouter {

  @GET
  @POST
  @Role("some")
  public String getIt(Route route) {
    assertEquals("some", route.attribute("role"));
    return "Got it!";
  }

  @GET
  @Path("/subpath")
  public String subpath() {
    return "OK";
  }

  @DELETE
  @Path("/void")
  public void noContent() {}

  @GET
  @Path("/voidwriter")
  public void writer(Context ctx) throws Exception {
    LoggerFactory.getLogger(getClass()).info("blocking");
    ctx.responseWriter(
        writer -> {
          writer.println("writer");
        });
  }
}
