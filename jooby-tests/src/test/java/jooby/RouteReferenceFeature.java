package jooby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.http.client.fluent.Request;
import org.junit.Test;

public class RouteReferenceFeature extends ServerFeature {

  {

    get("/", (req, res) -> {
      Route route = req.route();
      assertNotNull(route);
      assertEquals(0, route.index());
      assertEquals("route0", route.name());
      assertEquals("GET", route.verb());
      assertEquals("/", route.path());
      assertEquals("/", route.pattern());
      res.send("done");
    });

    get("/:var", (req, res) -> {
      Route route = req.route();
      assertNotNull(route);
      assertEquals(1, route.index());
      assertEquals("route1", route.name());
      assertEquals("GET", route.verb());
      assertEquals("/" + req.param("var").stringValue(), route.path());
      assertEquals("/:var", route.pattern());
      res.send("done");
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
