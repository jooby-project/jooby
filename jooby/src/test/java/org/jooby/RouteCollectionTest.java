package org.jooby;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.jooby.Route.Collection;
import org.jooby.Route.Definition;
import org.junit.Test;

public class RouteCollectionTest {

  @Test
  public void renderer() {
    Collection col = new Route.Collection(new Route.Definition("*", "*", (req, rsp, chain) -> {
    }))
        .renderer("json");

    assertEquals("json", col.renderer());
  }

  @Test
  public void attr() {
    Definition def = new Route.Definition("*", "*", (req, rsp, chain) -> {
    });
    new Route.Collection(def)
        .attr("foo", "bar");

    assertEquals("bar", def.attributes().get("foo"));
  }

  @Test
  public void excludes() {
    Definition def = new Route.Definition("*", "*", (req, rsp, chain) -> {
    });
    new Route.Collection(def)
        .excludes("/path");

    assertEquals(Arrays.asList("/path"), def.excludes());
  }

}
