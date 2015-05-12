package org.jooby;

import org.jooby.mvc.Consumes;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.mvc.Produces;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ContentNegotiationFeature extends ServerFeature {

  @Path("/r")
  public static class Resource {

    @Path("/any")
    @GET
    public Result any() {
      return Results
          .when("text/html", () -> Results.html("test").put("this", "body"))
          .when("*/*", () -> "body");
    }

    @Path("/html")
    @GET
    @Produces("text/html")
    public View html() {
      return Results.html("test").put("this", "body");
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

    renderer(BodyConverters.toHtml);

    renderer(BodyConverters.toJson);

    get("/any", req ->
        Results
            .when("text/html", () -> Results.html("test").put("this", "body"))
            .when("*/*", () -> "body"));

    get("/status", req ->
        Results
            .when("*", () -> Status.NOT_ACCEPTABLE));

    get("/like", req ->
        Results
            .when("text/html", () -> Results.html("test").put("this", "body"))
            .when("application/json", () -> "body"));

    get("/html", (req, resp) -> resp.send(Results.html("test").put("this", "body")))
        .produces(MediaType.html);

    get("/json", (req, resp) -> resp.send("body"))
        .produces(MediaType.json)
        .consumes(MediaType.json);

    use(Resource.class);
  }

  private static final String CHROME_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8";

  @Test
  public void chromeAccept() throws Exception {
    request()
        .get("/any")
        .header("Accept", CHROME_ACCEPT)
        .expect("<html><body>test: {this=body}</body></html>");

    request()
        .get("/r/any")
        .header("Accept", CHROME_ACCEPT)
        .expect("<html><body>test: {this=body}</body></html>");
  }

  @Test
  public void htmlAccept() throws Exception {
    request()
        .get("/any")
        .header("Accept", "text/html")
        .expect("<html><body>test: {this=body}</body></html>");

    request()
        .get("/r/any")
        .header("Accept", "text/html")
        .expect("<html><body>test: {this=body}</body></html>");

    request()
        .get("/html")
        .header("Accept", CHROME_ACCEPT)
        .expect("<html><body>test: {this=body}</body></html>");

    request()
        .get("/r/html")
        .header("Accept", CHROME_ACCEPT)
        .expect("<html><body>test: {this=body}</body></html>");

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
  public void jsonAccept() throws Exception {
    request()
        .get("/any")
        .header("Accept", "application/json")
        .expect("{\"body\": \"body\"}");

    request()
        .get("/r/any")
        .header("Accept", "application/json")
        .expect("{\"body\": \"body\"}");

    request()
        .get("/json")
        .header("Accept", "application/json")
        .expect("{\"body\": \"body\"}");

    request()
        .get("/r/json")
        .header("Accept", "application/json")
        .expect("{\"body\": \"body\"}");

    request()
        .get("/json")
        .header("Accept", CHROME_ACCEPT)
        .expect("{\"body\": \"body\"}");

    request()
        .get("/r/json")
        .header("Accept", CHROME_ACCEPT)
        .expect("{\"body\": \"body\"}");

    request()
        .get("/html")
        .header("Accept", "application/json")
        .expect(406);

    request()
        .get("/r/html")
        .header("Accept", "application/json")
        .expect(406);

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
  public void jsonConsume() throws Exception {
    request()
        .get("/json")
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .expect("{\"body\": \"body\"}");

    request()
        .get("/r/json")
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .expect("{\"body\": \"body\"}");

    request()
        .get("/json")
        .header("Content-Type", "application/xml")
        .header("Accept", "application/json")
        .expect(415);

    request()
        .get("/r/json")
        .header("Content-Type", "application/xml")
        .header("Accept", "application/json")
        .expect(415);

  }

  @Test
  public void fallback() throws Exception {
    request()
        .get("/any")
        .header("Accept", "application/json")
        .expect("{\"body\": \"body\"}");
  }

  @Test
  public void like() throws Exception {
//    request()
//        .get("/like")
//        .header("Accept", "application/*+json")
//        .expect("{\"body\": \"body\"}");

    request()
        .get("/like")
        .header("Accept", "application/xml")
        .expect(406);
  }

  @Test
  public void status() throws Exception {
    request()
        .get("/status")
        .header("Content-Type", "application/xml")
        .expect(406);
  }

}
