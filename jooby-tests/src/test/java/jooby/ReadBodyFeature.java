package jooby;

import static org.junit.Assert.assertEquals;
import jooby.mvc.Body;
import jooby.mvc.Consumes;
import jooby.mvc.GET;
import jooby.mvc.POST;
import jooby.mvc.Path;

import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
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

    use(new JoobyModule() {
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

    route(Resource.class);
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
