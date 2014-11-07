package org.jooby.test;

import static org.junit.Assert.assertEquals;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.Body;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.FilterFeature.HttpResponseValidator;
import org.junit.Test;

public class RedirectFeature extends ServerFeature {

  @Path("/r")
  public static class Resource {

    @Path("/redirect")
    @GET
    public Body redirect() {
      return Body.redirect("/r/here");
    }

    @Path("/seeOther")
    @GET
    public Body seeOther() {
      return Body.seeOther("/r/here");
    }

    @Path("/tempRedirect")
    @GET
    public Body tempRedirect() {
      return Body.tempRedirect("/r/here");
    }

    @Path("/moved")
    @GET
    public Body moved() {
      return Body.moved("/r/here");
    }

    @Path("/here")
    @GET
    public String here() {
      return "done";
    }

  }

  {

    get("/l1/l2/l3", redirect("/l1"));

    get("/l1", (req, rsp) -> rsp.send(req.path()));

    get("/blog/admin", redirect("post/new"));

    get("/blog/admin/post/new", (req, rsp) -> rsp.send(req.path()));

    get("/blog/post/new", (req, rsp) -> rsp.send(req.path()));

    get("/d1/d2/d3", redirect(".."));

    get("/d1/d2/d3b", redirect("../d2"));

    get("/d1/d2/d3c", redirect("./d2"));

    get("/back", redirect("back"));

    get("/d1", (req, rsp) -> rsp.send(req.path()));

    get("/d1/d2", (req, rsp) -> rsp.send(req.path()));

    get("/d1/d2/d2", (req, rsp) -> rsp.send(req.path()));

    get("/", (req, rsp) -> rsp.send(req.path()));

    use(Resource.class);
  }

  @Test
  public void redirect() throws Exception {
    assertEquals("done",
        execute(GET(uri("r/redirect")), (response) -> {
          assertEquals(200, response.getStatusLine().getStatusCode());
        }));
  }

  @Test
  public void redirectURI() throws Exception {
    assertEquals("/l1",
        execute(GET(uri("/l1/l2/l3")), (response) -> {
          assertEquals(200, response.getStatusLine().getStatusCode());
        }));

    assertEquals("/blog/admin/post/new",
        execute(GET(uri("/blog/admin/")), (response) -> {
          assertEquals(200, response.getStatusLine().getStatusCode());
        }));

    assertEquals("/blog/post/new",
        execute(GET(uri("/blog/admin")), (response) -> {
          assertEquals(200, response.getStatusLine().getStatusCode());
        }));

    assertEquals("/d1",
        execute(GET(uri("/d1/d2/d3")), (response) -> {
          assertEquals(200, response.getStatusLine().getStatusCode());
        }));

    assertEquals("/d1/d2",
        execute(GET(uri("/d1/d2/d3b")), (response) -> {
          assertEquals(200, response.getStatusLine().getStatusCode());
        }));

    assertEquals("/d1/d2/d2",
        execute(GET(uri("/d1/d2/d3c")), (response) -> {
          assertEquals(200, response.getStatusLine().getStatusCode());
        }));

    assertEquals("/",
        execute(GET(uri("/back")), (response) -> {
          assertEquals(200, response.getStatusLine().getStatusCode());
        }));

    assertEquals("/l1",
        execute(GET(uri("/back")).addHeader("Referer", "/l1"), (response) -> {
          assertEquals(200, response.getStatusLine().getStatusCode());
        }));
  }

  @Test
  public void seeOther() throws Exception {
    assertEquals("done",
        execute(GET(uri("r/seeOther")), (response) -> {
          assertEquals(200, response.getStatusLine().getStatusCode());
        }));
  }

  @Test
  public void temp() throws Exception {
    assertEquals("done",
        execute(GET(uri("r/tempRedirect")), (response) -> {
          assertEquals(200, response.getStatusLine().getStatusCode());
        }));
  }

  @Test
  public void moved() throws Exception {
    assertEquals("done",
        execute(GET(uri("r/moved")), (response) -> {
          assertEquals(200, response.getStatusLine().getStatusCode());
        }));
  }

  private static Request GET(final URIBuilder uri) throws Exception {
    return Request.Get(uri.build());
  }

  private static Object execute(final Request request, final HttpResponseValidator validator)
      throws Exception {
    HttpResponse resp = request.execute().returnResponse();
    validator.validate(resp);
    return EntityUtils.toString(resp.getEntity());
  }
}
