package examples;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;

@Path("/attr")
public class MvcAttributes {

  @GET
  @Path("/secured/subpath")
  @Role(value = "Admin", level = "two")
  public String subpath() {
    return "Got it!!";
  }

  @GET
  @Path("/secured/otherpath")
  @Role("User")
  public String otherpath() {
    return "OK!";
  }
}
