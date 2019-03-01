package io.jooby.internal.mvc;

import io.jooby.annotations.GET;
import io.jooby.annotations.POST;
import io.jooby.annotations.Path;

@Path("/")
public class InstanceRouter {

  @GET
  @POST
  public String getIt() {
    return "Got it!";
  }

  @GET
  @Path("/subpath")
  public String subpath() {
    return "OK";
  }

  @GET
  @Path("/void")
  public void noContent() {

  }
}
