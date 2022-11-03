package source;

import io.jooby.Context;

import javax.ws.rs.*;

@Path("/jaxrs")
public class JaxrsController {

  @GET
  public String doGet(Context ctx) {
    return "doGet";
  }

  @POST @Path("/post")
  public String doPost(Context ctx) {
    return "doPost";
  }

  @GET @Path("/query")
  public String doGet(@QueryParam("q1") String queryParam) {
    return queryParam;
  }

  @PUT @Path("/put/{id}")
  public String doPut(@PathParam("id") String id) {
    return id;
  }
}
