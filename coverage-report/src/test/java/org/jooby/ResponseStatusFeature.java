package org.jooby;

import org.jooby.MediaType;
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
    request()
        .get("/missing")
        .expect(404);
  }

  @Test
  public void methodNotAllowed() throws Exception {
    request()
        .post("/notAllowed")
        .expect(405);

    request()
        .post("/r/notAllowed")
        .expect(405);

  }

  @Test
  public void notAcceptable() throws Exception {
    request()
        .get("/json")
        .header("Accept", "text/html")
        .expect(406);

    request()
        .get("/r/json")
        .header("Accept", "text/html")
        .expect(406);
  }

  @Test
  public void unsupportedMediaType() throws Exception {
    request()
        .get("/json")
        .header("Content-Type", "text/html")
        .expect(415);

    request()
        .get("/r/json")
        .header("Content-Type", "text/html")
        .expect(415);

  }

}
