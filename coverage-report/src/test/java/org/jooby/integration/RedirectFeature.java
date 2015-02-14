package org.jooby.integration;

import org.jooby.Body;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
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
