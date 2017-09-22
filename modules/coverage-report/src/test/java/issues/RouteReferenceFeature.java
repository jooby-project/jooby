package issues;

import org.jooby.Route;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
      rsp.send(req.toString());
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
        .expect(
            "| Method | Path | Source                          | Name       | Pattern | Consumes | Produces |\n"
                + "|--------|------|---------------------------------|------------|---------|----------|----------|\n"
                + "| GET    | /    | issues.RouteReferenceFeature:14 | /anonymous | /       | [*/*]    | [*/*]    |");
  }

  @Test
  public void varRoute() throws Exception {
    request()
        .get("/xx")
        .expect("done");
  }

}
