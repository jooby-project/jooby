package org.jooby.integration;

import static org.junit.Assert.assertEquals;

import org.apache.http.client.fluent.Request;
import org.jooby.Body;
import org.jooby.Jooby;
import org.jooby.MediaType;
import org.jooby.Env;
import org.jooby.Status;
import org.jooby.View;
import org.jooby.mvc.Consumes;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.mvc.Produces;
import org.jooby.test.ServerFeature;
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

    use(new Jooby.Module() {

      @Override
      public void configure(final Env mode, final Config config, final Binder binder)
          throws Exception {
        Multibinder.newSetBinder(binder, Body.Formatter.class)
        .addBinding().toInstance(BodyConverters.toHtml);

        Multibinder.newSetBinder(binder, Body.Formatter.class)
        .addBinding().toInstance(BodyConverters.toJson);
      }
    });

    get("/any", (req, resp) ->
        resp.format()
            .when("text/html", () -> View.of("test", "body"))
            .when("*/*", () -> "body")
            .send());

    get("/status", (req, resp) ->
    resp.format()
        .when("*", () -> Status.NOT_ACCEPTABLE)
        .send());

    get("/like", (req, resp) ->
    resp.format()
        .when("text/html", () -> View.of("test", "body"))
        .when("application/json", () -> "body")
        .send());

    get("/html", (req, resp) -> resp.send(View.of("test", "body")))
        .produces(MediaType.html);

    get("/json", (req, resp) -> resp.send("body"))
        .produces(MediaType.json)
        .consumes(MediaType.json);

    use(Resource.class);
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

    assertStatus(Status.NOT_ACCEPTABLE, () -> {
      Request.Get(uri("json").build()).addHeader("Accept", "text/html").execute()
          .returnContent().asString();
    });

    assertStatus(Status.NOT_ACCEPTABLE, () -> {
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

    assertStatus(Status.NOT_ACCEPTABLE, () -> {
      Request.Get(uri("html").build()).addHeader("Accept", "application/json").execute()
          .returnContent().asString();
    });

    assertStatus(Status.NOT_ACCEPTABLE, () -> {
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

    assertStatus(Status.UNSUPPORTED_MEDIA_TYPE, () -> {
      Request.Get(uri("json").build()).addHeader("Content-Type", "application/xml").execute()
          .returnContent().asString();
    });

  }

  @Test
  public void fallback() throws Exception {
    assertEquals("{\"body\": \"body\"}",
        Request.Get(uri("any").build())
            .addHeader("Accept", "application/json").execute()
            .returnContent().asString());
  }

  @Test
  public void like() throws Exception {
    assertEquals("{\"body\": \"body\"}",
        Request.Get(uri("like").build())
            .addHeader("Accept", "application/*+json").execute()
            .returnContent().asString());
  }

  @Test
  public void status() throws Exception {
    assertStatus(Status.NOT_ACCEPTABLE, () -> {
      Request.Get(uri("status").build()).addHeader("Content-Type", "application/xml").execute()
          .returnContent().asString();
    });
  }

}
