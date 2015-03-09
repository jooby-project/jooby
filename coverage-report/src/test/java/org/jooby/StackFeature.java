package org.jooby;

import org.apache.http.HttpResponse;
import org.jooby.Response;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StackFeature extends ServerFeature {

  public interface HttpResponseValidator {
    public void validate(HttpResponse response);
  }

  @Path("/r")
  public static class Resource {

    /** The logging system. */
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Path("*")
    @org.jooby.mvc.GET
    public void xHeader(final Response resp) {
      log.info("X");
      resp.header("X", "x");
    }

    @Path("*")
    @org.jooby.mvc.GET
    public void yHeader(final Response resp) {
      log.info("Y");
      resp.header("Y", "y");
    }

    @Path("/")
    @org.jooby.mvc.GET
    public String root() throws Exception {
      return "root";
    }

    @Path("/subpath")
    @org.jooby.mvc.GET
    public String subpath() throws Exception {
      return "subpath";
    }

    @Path("/subpath/1")
    @org.jooby.mvc.GET
    public String subpath1() throws Exception {
      return "subpath/1";
    }

    @Path("/subpath/f.js")
    @org.jooby.mvc.GET
    public String subpathfjs() throws Exception {
      return "subpath/f.js";
    }

  }

  {

    get("*", (req, resp) -> {
      resp.header("X", "x");
    });
    get("*", (req, resp) -> {
      resp.header("Y", "y");
    });

    get("/", (req, resp) -> {
      resp.send("root");
    });

    get("/subpath", (req, resp) -> {
      resp.send("subpath");
    });

    get("/subpath/1", (req, resp) -> {
      resp.send("subpath/1");
    });

    get("/subpath/f.js", (req, resp) -> {
      resp.send("subpath/f.js");
    });

    use(Resource.class);
  }

  @Test
  public void rootStack() throws Exception {
    request()
        .get("/")
        .expect(200)
        .header("X", "x")
        .header("Y", "y");

    request()
        .get("/r")
        .expect(200)
        .header("X", "x")
        .header("Y", "y");

  }

  @Test
  public void subpathStack() throws Exception {
    request()
        .get("/subpath")
        .expect("subpath")
        .header("X", "x")
        .header("Y", "y");

    request()
        .get("/r/subpath")
        .expect("subpath")
        .header("X", "x")
        .header("Y", "y");

  }

  @Test
  public void subpath1Stack() throws Exception {
    request()
        .get("/subpath/1")
        .expect("subpath/1")
        .header("X", "x")
        .header("Y", "y");

    request()
        .get("/r/subpath/1")
        .expect("subpath/1")
        .header("X", "x")
        .header("Y", "y");

  }

  @Test
  public void subpathFStack() throws Exception {
    request()
        .get("/subpath/f.js")
        .expect("subpath/f.js")
        .header("X", "x")
        .header("Y", "y");

    request()
        .get("/r/subpath/f.js")
        .expect("subpath/f.js")
        .header("X", "x")
        .header("Y", "y");

  }

  @Test
  public void notFound() throws Exception {
    request()
        .get("/missing")
        .expect(404)
        .header("X", (String) null)
        .header("Y", (String) null);

    request()
        .get("/r/missing")
        .expect(404)
        .header("X", (String) null)
        .header("Y", (String) null);

  }

}
