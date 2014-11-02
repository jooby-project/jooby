package org.jooby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.http.client.fluent.Request;
import org.jooby.Route;
import org.junit.Test;

public class RouteReferenceFeature extends ServerFeature {

  {

    get("/", (req, rsp) -> {
      Route route = req.route();
      assertNotNull(route);
      assertEquals("anonymous", route.name());
      assertEquals("GET", route.verb().name());
      assertEquals("/", route.path());
      assertEquals("/", route.pattern());
      rsp.send("done");
    });

    get("/:var", (req, rsp) -> {
      Route route = req.route();
      assertNotNull(route);
      assertEquals("anonymous", route.name());
      assertEquals("GET", route.verb().name());
      assertEquals("/" + req.param("var").stringValue(), route.path());
      assertEquals("/:var", route.pattern());
      rsp.send("done");
    });

  }

  @Test
  public void rootRoute() throws Exception {
    assertEquals("done", Request.Get(uri("/").build())
        .execute().returnContent().asString());
  }

  @Test
  public void varRoute() throws Exception {
    assertEquals("done", Request.Get(uri("/xx").build())
        .execute().returnContent().asString());
  }

}
