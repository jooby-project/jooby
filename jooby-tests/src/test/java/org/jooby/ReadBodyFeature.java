package org.jooby;

import static org.junit.Assert.assertEquals;

import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.jooby.BodyConverter;
import org.jooby.Jooby;
import org.jooby.MediaType;
import org.jooby.Mode;
import org.jooby.mvc.Body;
import org.jooby.mvc.Consumes;
import org.jooby.mvc.GET;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;

public class ReadBodyFeature extends ServerFeature {

  @Path("/r")
  public static class Resource {

    @Path("/text")
    @POST
    public String text(@Body final String body) {
      return body;
    }

    @Path("/text")
    @GET
    public String emptyBody(@Body final String body) {
      return body;
    }

    @Path("/json")
    @POST
    @Consumes("application/json")
    public String json(@Body final String body) {
      return body;
    }
  }

  {

    use(new Jooby.Module() {
      @Override
      public void configure(final Mode mode, final Config config, final Binder binder)
          throws Exception {
        Multibinder<BodyConverter> converters = Multibinder.newSetBinder(binder,
            BodyConverter.class);
        converters.addBinding().toInstance(TestBodyConverter.JSON);
      }
    });

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
    assertEquals("{\"body\": \"\"}", Request.Get(uri("text").build()).execute().returnContent().asString());

    assertEquals("{\"body\": \"\"}", Request.Get(uri("r", "text").build()).execute().returnContent().asString());
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
