package org.jooby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class RouteReferenceFeature extends ServerFeature {

  {

    get("/", (req, rsp) -> {
      Route route = req.route();
      assertNotNull(route);
      assertEquals("/anonymous", route.name());
      assertEquals("GET", route.method());
      assertEquals("/", route.path());
      assertEquals("/", route.pattern());
      assertEquals("bar", route.attr("foo"));
      assertEquals(
          "| Method | Path | Source                             | Name       | Pattern | Consumes | Produces |\n"
              +
              "|--------|------|------------------------------------|------------|---------|----------|----------|\n"
              +
              "| GET    | /    | org.jooby.RouteReferenceFeature:13 | /anonymous | /       | [*/*]    | [*/*]    |"
              +
              "",
          req.toString());
      rsp.send("done");
    }).attr("foo", "bar");

    get("/:var", (req, rsp) -> {
      Route route = req.route();
      assertNotNull(route);
      assertEquals("/anonymous", route.name());
      assertEquals("GET", route.method());
      assertEquals("/" + req.param("var").value(), route.path());
      assertEquals("/:var", route.pattern());
      rsp.send("done");
    });

  }

  @Test
  public void rootRoute() throws Exception {
    request()
        .get("/")
        .expect("done");
  }

  @Test
  public void varRoute() throws Exception {
    request()
        .get("/xx")
        .expect("done");
  }

}
