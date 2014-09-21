package jooby;

import static org.junit.Assert.assertEquals;
import jooby.mvc.Consumes;
import jooby.mvc.GET;
import jooby.mvc.Path;
import jooby.mvc.Produces;

import org.apache.http.client.fluent.Request;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;

public class ContentNegotiationFeature extends ServerFeature {

  @Path("/r")
  public static class Resource {

    @Path("/any")
    @GET
    public String any() {
      return "body";
    }

    @Path("/html")
    @GET
    @Produces("text/html")
    public String html() {
      return "body";
    }

    @Path("/json")
    @GET
    @Produces("application/json")
    @Consumes("application/json")
    public String json() {
      return "body";
    }
  }

  {

    use(new JoobyModule() {

      @Override
      public void configure(final Mode mode, final Config config, final Binder binder)
          throws Exception {
        Multibinder<BodyConverter> converters = Multibinder.newSetBinder(binder,
            BodyConverter.class);
        converters.addBinding().toInstance(TestBodyConverter.HTML);
        converters.addBinding().toInstance(TestBodyConverter.JSON);
      }
    });

    get("/any", (req, resp) ->
        resp.when("text/html", () -> Viewable.of("test", "body"))
            .when("application/json", () -> "body")
            .send());

    get("/html", (req, resp) -> resp.send(Viewable.of("test", "body")))
        .produces(MediaType.html);

    get("/json", (req, resp) -> resp.send("body"))
        .produces(MediaType.json)
        .consumes(MediaType.json);

    route(Resource.class);
  }

  private static final String CHROME_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8";

  @Test
  public void chromeAccept() throws Exception {
    assertEquals("<html><body>test: body</body></html>",
        Request.Get(uri("any").build()).addHeader("Accept", CHROME_ACCEPT).execute()
            .returnContent().asString());

    assertEquals("<html><body>any: body</body></html>",
        Request.Get(uri("r", "any").build()).addHeader("Accept", CHROME_ACCEPT).execute()
            .returnContent().asString());
  }

  @Test
  public void htmlAccept() throws Exception {
    assertEquals("<html><body>test: body</body></html>",
        Request.Get(uri("any").build()).addHeader("Accept", "text/html").execute()
            .returnContent().asString());

    assertEquals("<html><body>any: body</body></html>",
        Request.Get(uri("r", "any").build()).addHeader("Accept", "text/html").execute()
            .returnContent().asString());

    assertEquals("<html><body>test: body</body></html>",
        Request.Get(uri("html").build()).addHeader("Accept", CHROME_ACCEPT).execute()
            .returnContent().asString());

    assertEquals("<html><body>html: body</body></html>",
        Request.Get(uri("r", "html").build()).addHeader("Accept", CHROME_ACCEPT).execute()
            .returnContent().asString());

    assertStatus(HttpStatus.NOT_ACCEPTABLE, () -> {
      Request.Get(uri("json").build()).addHeader("Accept", "text/html").execute()
          .returnContent().asString();
    });

    assertStatus(HttpStatus.NOT_ACCEPTABLE, () -> {
      Request.Get(uri("r", "json").build()).addHeader("Accept", "text/html").execute()
          .returnContent().asString();
    });
  }

  @Test
  public void jsonAccept() throws Exception {
    assertEquals("{\"body\": \"body\"}",
        Request.Get(uri("any").build()).addHeader("Accept", "application/json").execute()
            .returnContent().asString());

    assertEquals("{\"body\": \"body\"}",
        Request.Get(uri("r", "any").build()).addHeader("Accept", "application/json").execute()
            .returnContent().asString());

    assertEquals("{\"body\": \"body\"}",
        Request.Get(uri("json").build()).addHeader("Accept", "application/json").execute()
            .returnContent().asString());

    assertEquals("{\"body\": \"body\"}",
        Request.Get(uri("r", "json").build()).addHeader("Accept", "application/json").execute()
            .returnContent().asString());

    assertEquals("{\"body\": \"body\"}",
        Request.Get(uri("json").build()).addHeader("Accept", CHROME_ACCEPT).execute()
            .returnContent().asString());

    assertEquals("{\"body\": \"body\"}",
        Request.Get(uri("r", "json").build()).addHeader("Accept", CHROME_ACCEPT).execute()
            .returnContent().asString());

    assertStatus(HttpStatus.NOT_ACCEPTABLE, () -> {
      Request.Get(uri("html").build()).addHeader("Accept", "application/json").execute()
          .returnContent().asString();
    });

    assertStatus(HttpStatus.NOT_ACCEPTABLE, () -> {
      Request.Get(uri("r", "html").build()).addHeader("Accept", "application/json").execute()
          .returnContent().asString();
    });
  }

  @Test
  public void jsonConsume() throws Exception {
    assertEquals("{\"body\": \"body\"}",
        Request.Get(uri("json").build())
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json").execute()
            .returnContent().asString());

    assertEquals("{\"body\": \"body\"}",
        Request.Get(uri("r", "json").build())
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json").execute()
            .returnContent().asString());

    assertStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE, () -> {
      Request.Get(uri("json").build()).addHeader("Content-Type", "application/xml").execute()
          .returnContent().asString();
    });

  }

}
