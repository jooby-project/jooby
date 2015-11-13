package org.jooby;

import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class RedirectFeature extends ServerFeature {

  @Path("/r")
  public static class Resource {

    @Path("/redirect")
    @GET
    public Result redirect() {
      return Results.redirect("/r/here");
    }

    @Path("/seeOther")
    @GET
    public Result seeOther() {
      return Results.seeOther("/r/here");
    }

    @Path("/tempRedirect")
    @GET
    public Result tempRedirect() {
      return Results.tempRedirect("/r/here");
    }

    @Path("/moved")
    @GET
    public Result moved() {
      return Results.moved("/r/here");
    }

    @Path("/here")
    @GET
    public String here() {
      return "done";
    }

  }

  {

    get("/l1/l2/l3", () -> Results.redirect("/l1"));

    get("/l1", (req, rsp) -> rsp.send(req.path()));

    get("/blog/admin", () -> Results.redirect("post/new"));

    get("/blog/admin/post/new", (req, rsp) -> rsp.send(req.path()));

    get("/blog/post/new", (req, rsp) -> rsp.send(req.path()));

    get("/d1/d2/d3", () -> Results.redirect(".."));

    get("/d1/d2/d3b", () -> Results.redirect("../d2"));

    get("/d1/d2/d3c", () -> Results.redirect("./d2"));

    get("/back", () -> Results.redirect("back"));

    get("/d1", (req, rsp) -> rsp.send(req.path()));

    get("/d1/d2", (req, rsp) -> rsp.send(req.path()));

    get("/d1/d2/d2", (req, rsp) -> rsp.send(req.path()));

    get("/", (req, rsp) -> rsp.send(req.path()));

    use(Resource.class);
  }

  @Test
  public void redirect() throws Exception {
    request()
        .get("/r/redirect")
        .expect("done")
        .expect(200);
  }

  @Test
  public void redirectURI() throws Exception {
    request()
        .get("/l1/l2/l3")
        .expect("/l1")
        .expect(200);

    request()
        .get("/blog/admin/")
        .expect("/blog/admin/post/new")
        .expect(200);

    request()
        .get("/blog/admin")
        .expect("/blog/post/new")
        .expect(200);

    request()
        .get("/d1/d2/d3")
        .expect("/d1")
        .expect(200);

    request()
        .get("/d1/d2/d3b")
        .expect("/d1/d2")
        .expect(200);

    request()
        .get("/d1/d2/d3c")
        .expect("/d1/d2/d2")
        .expect(200);

    request()
        .get("/back")
        .expect("/")
        .expect(200);

    request()
        .get("/back")
        .header("Referer", "/l1")
        .expect("/l1")
        .expect(200);
  }

  @Test
  public void seeOther() throws Exception {
    request()
        .get("/r/seeOther")
        .expect("done")
        .expect(200);
  }

  @Test
  public void temp() throws Exception {
    request()
        .get("/r/tempRedirect")
        .expect("done")
        .expect(200);
  }

  @Test
  public void moved() throws Exception {
    request()
        .get("/r/moved")
        .expect("done")
        .expect(200);

  }

}
