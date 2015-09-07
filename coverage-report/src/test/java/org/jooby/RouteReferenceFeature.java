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
      assertEquals("GET /\n" +
          "  pattern: /\n" +
          "  name: /anonymous\n" +
          "  vars: {}\n" +
          "  consumes: [*/*]\n" +
          "  produces: [*/*]\n", req.toString());
      rsp.send("done");
    });

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
