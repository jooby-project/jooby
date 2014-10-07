package jooby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.client.fluent.Request;
import org.junit.Test;

public class RouteReferenceFeature extends ServerFeature {

  static {
    /**
     * reset index when running multiple tests at once, either from IDE or Maven.
     * Index is produced by a static var and when running multiples tests the number of routes
     * wont matches.
     *
     * I'm sure something better can be done... later
     */
    try {
      Field field = RouteDefinition.class.getDeclaredField("INDEX");
      field.setAccessible(true);
      AtomicInteger index = (AtomicInteger) field.get(null);
      index.set(-1);
    } catch (Exception ex) {
      // ignored
      ex.printStackTrace();
    }
  }

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
