package source;

import io.jooby.Context;
import io.jooby.annotations.GET;
import io.jooby.annotations.POST;
import io.jooby.annotations.Path;

@Path("/")
public class GetPostRoute {

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

  @GET
  @Path("/voidwriter")
  public void writer(Context ctx) throws Exception {
    ctx.responseWriter(writer -> {
      writer.println("writer");
    });
  }
}
