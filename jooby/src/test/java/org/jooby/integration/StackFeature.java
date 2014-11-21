package org.jooby.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
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
    assertEquals("root", execute(GET(uri("/")), (response) -> {
      assertEquals("x", response.getFirstHeader("X").getValue());
      assertEquals("y", response.getFirstHeader("Y").getValue());
    }));

    assertEquals("root", execute(GET(uri("/r")), (response) -> {
      assertEquals("x", response.getFirstHeader("X").getValue());
      assertEquals("y", response.getFirstHeader("Y").getValue());
    }));
  }

  @Test
  public void subpathStack() throws Exception {
    assertEquals("subpath", execute(GET(uri("/subpath")), (response) -> {
      assertEquals("x", response.getFirstHeader("X").getValue());
      assertEquals("y", response.getFirstHeader("Y").getValue());
    }));

    assertEquals("subpath", execute(GET(uri("/r/subpath")), (response) -> {
      assertEquals("x", response.getFirstHeader("X").getValue());
      assertEquals("y", response.getFirstHeader("Y").getValue());
    }));
  }

  @Test
  public void subpath1Stack() throws Exception {
    assertEquals("subpath/1", execute(GET(uri("/subpath/1")), (response) -> {
      assertEquals("x", response.getFirstHeader("X").getValue());
      assertEquals("y", response.getFirstHeader("Y").getValue());
    }));

    assertEquals("subpath/1", execute(GET(uri("/r/subpath/1")), (response) -> {
      assertEquals("x", response.getFirstHeader("X").getValue());
      assertEquals("y", response.getFirstHeader("Y").getValue());
    }));
  }

  @Test
  public void subpathFStack() throws Exception {
    assertEquals("subpath/f.js", execute(GET(uri("/subpath/f.js")), (response) -> {
      assertEquals("x", response.getFirstHeader("X").getValue());
      assertEquals("y", response.getFirstHeader("Y").getValue());
    }));

    assertEquals("subpath/f.js", execute(GET(uri("/r/subpath/f.js")), (response) -> {
      assertEquals("x", response.getFirstHeader("X").getValue());
      assertEquals("y", response.getFirstHeader("Y").getValue());
    }));
  }

  @Test
  public void notFound() throws Exception {
//    execute(GET(uri("/missing")), (response) -> {
//      assertNull(response.getFirstHeader("X"));
//      assertNull(response.getFirstHeader("Y"));
//      assertEquals(404, response.getStatusLine().getStatusCode());
//    });

    execute(GET(uri("/r/missing")), (response) -> {
      assertNull(response.getFirstHeader("X"));
      assertNull(response.getFirstHeader("Y"));
      assertEquals(404, response.getStatusLine().getStatusCode());
    });
  }

  private static Request GET(final URIBuilder uri) throws Exception {
    return Request.Get(uri.build());
  }

  private static String execute(final Request request, final HttpResponseValidator validator)
      throws Exception {
    HttpResponse resp = request.execute().returnResponse();
    validator.validate(resp);
    return EntityUtils.toString(resp.getEntity());
  }

}
