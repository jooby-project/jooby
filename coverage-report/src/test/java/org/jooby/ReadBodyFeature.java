package org.jooby;

import org.jooby.MediaType;
import org.jooby.mvc.Consumes;
import org.jooby.mvc.GET;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ReadBodyFeature extends ServerFeature {

  @Path("/r")
  public static class Resource {

    @Path("/text")
    @POST
    public String text(final String body) {
      return body;
    }

    @Path("/text")
    @GET
    public String emptyBody(final String body) {
      return body;
    }

    @Path("/json")
    @POST
    @Consumes("application/json")
    public String json(final String body) {
      return body;
    }
  }

  {

    use(BodyConverters.toJson);
    use(BodyConverters.fromJson);

    post("/text", (req, resp) -> resp.send(req.body(String.class)));

    get("/text", (req, resp) -> resp.send(req.body(String.class)));

    post("/json", (req, resp) -> resp.send(req.body(String.class)))
        .consumes(MediaType.json);

    post("/len", (req, resp) -> resp.send(req.length()));

    use(Resource.class);
  }

  @Test
  public void textBody() throws Exception {
    request()
        .post("/text")
        .body("..", "*/*")
        .expect("{\"body\": \"..\"}");

    request()
        .post("/r/text")
        .body("..x", "*/*")
        .expect("{\"body\": \"..x\"}");
  }

  @Test
  public void len() throws Exception {
    request()
        .post("/len")
        .body("..", "*/*")
        .expect("{\"body\": \"2\"}");
  }

  @Test
  public void emptyBody() throws Exception {
    request()
        .post("/text")
        .expect(400);

    request()
        .post("/r/text")
        .expect(400);
  }

  @Test
  public void jsonBody() throws Exception {
    request()
        .post("/json")
        .header("Content-Type", "application/json")
        .body("{\"x\": \"y\"}", "application/json")
        .expect("{\"body\": \"{\"x\": \"y\"}\"}");

    request()
        .post("/r/json")
        .header("Content-Type", "application/json")
        .body("{\"x\": \"y\"}", "application/json")
        .expect("{\"body\": \"{\"x\": \"y\"}\"}");

  }

}
