package org.jooby.integration;

import org.apache.http.client.fluent.Request;
import org.jooby.MediaType;
import org.jooby.Status;
import org.jooby.mvc.Consumes;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.mvc.Produces;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ResponseStatusFeature extends ServerFeature {

  @Path("/r")
  public static class Resource {

    @Path("/notAllowed")
    @GET
    public String notAllowed() {
      return "not allowed";
    }

    @Path("/json")
    @GET
    @Produces("application/json")
    @Consumes("application/json")
    public String json() {
      return "{}";
    }

  }

  {

    get("/notAllowed", (req, resp) -> resp.send("GET"));

    get("/json", (req, resp) -> resp.send("{}"))
        .consumes(MediaType.json)
        .produces(MediaType.json);

    use(Resource.class);
  }

  @Test
  public void notFound() throws Exception {
    assertStatus(Status.NOT_FOUND, () -> Request.Get(uri("missing").build()).execute()
        .returnContent().asString());
  }

  @Test
  public void methodNotAllowed() throws Exception {
    assertStatus(Status.METHOD_NOT_ALLOWED, () -> Request.Post(uri("/notAllowed").build())
        .execute().returnContent().asString());

    assertStatus(Status.METHOD_NOT_ALLOWED, () -> Request.Post(uri("/r/notAllowed").build())
        .execute().returnContent().asString());
  }

  @Test
  public void notAcceptable() throws Exception {
    assertStatus(Status.NOT_ACCEPTABLE, () -> Request.Get(uri("/json").build())
        .addHeader("Accept", "text/html")
        .execute().returnContent().asString());

    assertStatus(Status.NOT_ACCEPTABLE, () -> Request.Get(uri("/r/json").build())
        .addHeader("Accept", "text/html")
        .execute().returnContent().asString());
  }

  @Test
  public void unsupportedMediaType() throws Exception {
    assertStatus(Status.UNSUPPORTED_MEDIA_TYPE, () -> Request.Get(uri("/json").build())
        .addHeader("Content-Type", "text/html")
        .execute().returnContent().asString());

    assertStatus(Status.UNSUPPORTED_MEDIA_TYPE, () -> Request.Get(uri("/r/json").build())
        .addHeader("Content-Type", "text/html")
        .execute().returnContent().asString());
  }

}
