package org.jooby;

import java.util.List;
import java.util.Optional;

import org.jooby.mvc.Consumes;
import org.jooby.mvc.DELETE;
import org.jooby.mvc.GET;
import org.jooby.mvc.PATCH;
import org.jooby.mvc.POST;
import org.jooby.mvc.PUT;
import org.jooby.mvc.Path;
import org.jooby.mvc.Produces;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class HandlersFeature extends ServerFeature {

  @Path("/r")
  public static class Resource {

    @Path({"/id", "/id/:id" })
    @GET
    @Produces("text/plain")
    @Consumes("text/plain")
    public Object optionalVar(final Request req, final Optional<String> id) {
      return id + ";consumes" + req.route().consumes() + ";produces="
          + req.route().produces();
    }

    @Path({"/p1", "/p2", "/p3" })
    @GET
    @POST
    @PUT
    @DELETE
    @PATCH
    public Object get(final Request req) {
      return req.path();
    }
  }

  {

    use(new BodyFormatter() {

      @Override
      public List<MediaType> types() {
        return ImmutableList.of(MediaType.text);
      }

      @Override
      public void format(final Object body, final BodyFormatter.Context writer) throws Exception {
        writer.text(w -> w.write(body.toString()));

      }

      @Override
      public boolean canFormat(final Class<?> type) {
        return true;
      }
    });
    get("/id", "/id/:id", req ->
        req.param("id").toOptional(String.class) + ";consumes" + req.route().consumes()
            + ";produces=" + req.route().produces() + ";name=" + req.route().name())
        .consumes(MediaType.plain)
        .consumes(MediaType.plain.name())
        .produces(MediaType.plain)
        .produces(MediaType.plain.name())
        .name("xId");

    /**
     * GETs
     */
    get("/zp1", "/zp2", () -> require(Request.class).path());

    get("/zp1", "/zp2", "zp3", () -> require(Request.class).path());

    get("/p1", "/p2", req -> req.path());

    get("/p1", "/p2", "/p3", req -> req.path());

    get("/rr1", "/rr2", (req, rsp) -> rsp.send(req.path()));

    get("/rr1", "/rr2", "/rr3", (req, rsp) -> rsp.send(req.path()));

    get("/f1", "/f2", (req, rsp, chain) -> rsp.send(req.path()));

    get("/f1", "/f2", "/f3", (req, rsp, chain) -> rsp.send(req.path()));

    /**
     * POSTs
     */
    post("/zp1", "/zp2", () -> require(Request.class).path());

    post("/zp1", "/zp2", "zp3", () -> require(Request.class).path());

    post("/p1", "/p2", req -> req.path());

    post("/p1", "/p2", "/p3", req -> req.path());

    post("/rr1", "/rr2", (req, rsp) -> rsp.send(req.path()));

    post("/rr1", "/rr2", "/rr3", (req, rsp) -> rsp.send(req.path()));

    post("/f1", "/f2", (req, rsp, chain) -> rsp.send(req.path()));

    post("/f1", "/f2", "/f3", (req, rsp, chain) -> rsp.send(req.path()));

    /**
     * PUTs
     */
    put("/zp1", "/zp2", () -> require(Request.class).path());

    put("/zp1", "/zp2", "zp3", () -> require(Request.class).path());

    put("/p1", "/p2", req -> req.path());

    put("/p1", "/p2", "/p3", req -> req.path());

    put("/rr1", "/rr2", (req, rsp) -> rsp.send(req.path()));

    put("/rr1", "/rr2", "/rr3", (req, rsp) -> rsp.send(req.path()));

    put("/f1", "/f2", (req, rsp, chain) -> rsp.send(req.path()));

    put("/f1", "/f2", "/f3", (req, rsp, chain) -> rsp.send(req.path()));

    /**
     * PATCHs
     */
    patch("/zp1", "/zp2", () -> require(Request.class).path());

    patch("/zp1", "/zp2", "zp3", () -> require(Request.class).path());

    patch("/p1", "/p2", req -> req.path());

    patch("/p1", "/p2", "/p3", req -> req.path());

    patch("/rr1", "/rr2", (req, rsp) -> rsp.send(req.path()));

    patch("/rr1", "/rr2", "/rr3", (req, rsp) -> rsp.send(req.path()));

    patch("/f1", "/f2", (req, rsp, chain) -> rsp.send(req.path()));

    patch("/f1", "/f2", "/f3", (req, rsp, chain) -> rsp.send(req.path()));

    /**
     * DELETEs
     */
    delete("/zp1", "/zp2", () -> require(Request.class).path());

    delete("/zp1", "/zp2", "zp3", () -> require(Request.class).path());

    delete("/p1", "/p2", req -> req.path());

    delete("/p1", "/p2", "/p3", req -> req.path());

    delete("/rr1", "/rr2", (req, rsp) -> rsp.send(req.path()));

    delete("/rr1", "/rr2", "/rr3", (req, rsp) -> rsp.send(req.path()));

    delete("/f1", "/f2", (req, rsp, chain) -> rsp.send(req.path()));

    delete("/f1", "/f2", "/f3", (req, rsp, chain) -> rsp.send(req.path()));

    use(Resource.class);

  }

  @Test
  public void shouldSupportOptionalPathVar() throws Exception {
    request()
        .get("/id")
        .header("Content-Type", "text/plain")
        .expect("Optional.empty;consumes[text/plain];produces=[text/plain];name=xId");

    request()
        .get("/id/678")
        .header("Content-Type", "text/plain")
        .expect("Optional[678];consumes[text/plain];produces=[text/plain];name=xId");

    request()
        .get("/r/id")
        .header("Content-Type", "text/plain")
        .expect("Optional.empty;consumes[text/plain];produces=[text/plain]");

    request()
        .get("/r/id/678")
        .header("Content-Type", "text/plain")
        .expect("Optional[678];consumes[text/plain];produces=[text/plain]");
  }

  @Test
  public void get() throws Exception {
    request()
        .get("/p1")
        .expect("/p1");

    request()
        .get("/p2")
        .expect("/p2");

    request()
        .get("/p3")
        .expect("/p3");

    request()
        .get("/zp1")
        .expect("/zp1");

    request()
        .get("/zp2")
        .expect("/zp2");

    request()
        .get("/zp3")
        .expect("/zp3");

    request()
        .get("/rr1")
        .expect("/rr1");

    request()
        .get("/rr2")
        .expect("/rr2");

    request()
        .get("/rr3")
        .expect("/rr3");

    request()
        .get("/r/p1")
        .expect("/r/p1");

    request()
        .get("/r/p2")
        .expect("/r/p2");

    request()
        .get("/r/p3")
        .expect("/r/p3");
  }

  @Test
  public void post() throws Exception {
    request()
        .post("/p1")
        .expect("/p1");

    request()
        .post("/p2")
        .expect("/p2");

    request()
        .post("/p3")
        .expect("/p3");

    request()
        .post("/zp1")
        .expect("/zp1");

    request()
        .post("/zp2")
        .expect("/zp2");

    request()
        .post("/zp3")
        .expect("/zp3");

    request()
        .post("/rr1")
        .expect("/rr1");

    request()
        .post("/rr2")
        .expect("/rr2");

    request()
        .post("/rr3")
        .expect("/rr3");

    request()
        .post("/r/p1")
        .expect("/r/p1");

    request()
        .post("/r/p2")
        .expect("/r/p2");

    request()
        .post("/r/p3")
        .expect("/r/p3");
  }

  @Test
  public void put() throws Exception {
    request()
        .put("/p1")
        .expect("/p1");

    request()
        .put("/p2")
        .expect("/p2");

    request()
        .put("/p3")
        .expect("/p3");

    request()
        .put("/zp1")
        .expect("/zp1");

    request()
        .put("/zp2")
        .expect("/zp2");

    request()
        .put("/zp3")
        .expect("/zp3");

    request()
        .put("/rr1")
        .expect("/rr1");

    request()
        .put("/rr2")
        .expect("/rr2");

    request()
        .put("/rr3")
        .expect("/rr3");

    request()
        .put("/r/p1")
        .expect("/r/p1");

    request()
        .put("/r/p2")
        .expect("/r/p2");

    request()
        .put("/r/p3")
        .expect("/r/p3");
  }

  @Test
  public void delete() throws Exception {
    request()
        .delete("/p1")
        .expect("/p1");

    request()
        .delete("/p2")
        .expect("/p2");

    request()
        .delete("/p3")
        .expect("/p3");

    request()
        .delete("/zp1")
        .expect("/zp1");

    request()
        .delete("/zp2")
        .expect("/zp2");

    request()
        .delete("/zp3")
        .expect("/zp3");

    request()
        .delete("/rr1")
        .expect("/rr1");

    request()
        .delete("/rr2")
        .expect("/rr2");

    request()
        .delete("/rr3")
        .expect("/rr3");

    request()
        .delete("/r/p1")
        .expect("/r/p1");

    request()
        .delete("/r/p2")
        .expect("/r/p2");

    request()
        .delete("/r/p3")
        .expect("/r/p3");
  }

  @Test
  public void patch() throws Exception {
    request()
        .patch("/p1")
        .expect("/p1");

    request()
        .patch("/p2")
        .expect("/p2");

    request()
        .patch("/p3")
        .expect("/p3");

    request()
        .patch("/zp1")
        .expect("/zp1");

    request()
        .patch("/zp2")
        .expect("/zp2");

    request()
        .patch("/zp3")
        .expect("/zp3");

    request()
        .patch("/rr1")
        .expect("/rr1");

    request()
        .patch("/rr2")
        .expect("/rr2");

    request()
        .patch("/rr3")
        .expect("/rr3");

    request()
        .patch("/r/p1")
        .expect("/r/p1");

    request()
        .patch("/r/p2")
        .expect("/r/p2");

    request()
        .patch("/r/p3")
        .expect("/r/p3");
  }
}
