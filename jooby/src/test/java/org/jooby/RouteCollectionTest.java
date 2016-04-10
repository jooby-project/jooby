package org.jooby;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.jooby.Route.Definition;
import org.junit.Test;

public class RouteCollectionTest {

  @Test
  public void renderer() {
    Definition def = new Route.Definition("*", "*", (req, rsp, chain) -> {
    });
    new Route.Collection(def)
        .renderer("json");

    assertEquals("json", def.attributes().get("renderer"));
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
