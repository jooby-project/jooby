package org.jooby.integration;

import static org.junit.Assert.assertEquals;

import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
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

    use(Resource.class);
  }

  @Test
  public void textBody() throws Exception {
    assertEquals("{\"body\": \"..\"}", Request.Post(uri("text").build())
        .bodyString("..", ContentType.WILDCARD).execute()
        .returnContent().asString());

    assertEquals("{\"body\": \"..x\"}", Request.Post(uri("r", "text").build())
        .bodyString("..x", ContentType.WILDCARD).execute()
        .returnContent().asString());
  }

  @Test
  public void emptyBody() throws Exception {
    assertEquals("{\"body\": \"\"}", Request.Post(uri("text").build()).execute().returnContent().asString());

    assertEquals("{\"body\": \"\"}", Request.Post(uri("r", "text").build()).execute().returnContent().asString());
  }

  @Test
  public void jsonBody() throws Exception {
    assertEquals(
        "{\"body\": \"{\"x\": \"y\"}\"}",
        Request.Post(uri("json").build()).addHeader("Content-Type", "application/json")
            .bodyString("{\"x\": \"y\"}", ContentType.APPLICATION_JSON).execute()
            .returnContent().asString());

    assertEquals(
        "{\"body\": \"{\"x\": \"yu\"}\"}",
        Request.Post(uri("r/json").build()).addHeader("Content-Type", "application/json")
            .bodyString("{\"x\": \"yu\"}", ContentType.APPLICATION_JSON).execute()
            .returnContent().asString());
  }

}
