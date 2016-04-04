package org.jooby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jooby.Route.Group;
import org.junit.Test;

public class RouteGroupTest {

  @Test
  public void all() {
    Group ns = new Route.Group("/ns");
    ns.all((req, rsp, chain) -> {
    });
    ns.all((req, rsp) -> {
    });
    ns.all(req -> "X");
    ns.all(() -> "X");

    assertEquals(4, ns.routes().size());
    matches(ns.routes(), "GET", "/ns");
    matches(ns.routes(), "POST", "/ns");
  }

  @Test
  public void allWithPath() {
    Group ns = new Route.Group("/ns");
    ns.all("/s", (req, rsp, chain) -> {
    });
    ns.all("/s", (req, rsp) -> {
    });
    ns.all("/s", req -> "X");
    ns.all("/s", () -> "X");

    assertEquals(4, ns.routes().size());
    matches(ns.routes(), "GET", "/ns/s");
    matches(ns.routes(), "POST", "/ns/s");
  }

  @Test
  public void get() {
    Group ns = new Route.Group("/ns");
    ns.get((req, rsp, chain) -> {
    });
    ns.get((req, rsp) -> {
    });
    ns.get(req -> "X");
    ns.get(() -> "X");

    assertEquals(4, ns.routes().size());
    matches(ns.routes(), "GET", "/ns");
    noMatches(ns.routes(), "POST", "/ns");
  }

  @Test
  public void getWithPath() {
    Group ns = new Route.Group("/ns");
    ns.get("/s", (req, rsp, chain) -> {
    });
    ns.get("/s", (req, rsp) -> {
    });
    ns.get("/s", req -> "X");
    ns.get("/s", () -> "X");

    assertEquals(4, ns.routes().size());
    matches(ns.routes(), "GET", "/ns/s");
    noMatches(ns.routes(), "POST", "/ns/s");
  }

  @Test
  public void post() {
    Group ns = new Route.Group("/ns");
    ns.post((req, rsp, chain) -> {
    });
    ns.post((req, rsp) -> {
    });
    ns.post(req -> "X");
    ns.post(() -> "X");

    assertEquals(4, ns.routes().size());
    matches(ns.routes(), "POST", "/ns");
    noMatches(ns.routes(), "GET", "/ns");
  }

  @Test
  public void postWithPath() {
    Group ns = new Route.Group("/ns");
    ns.post("/x", (req, rsp, chain) -> {
    });
    ns.post("/x", (req, rsp) -> {
    });
    ns.post("/x", req -> "X");
    ns.post("/x", () -> "X");

    assertEquals(4, ns.routes().size());
    matches(ns.routes(), "POST", "/ns/x");
    noMatches(ns.routes(), "GET", "/ns/x");
  }

  @Test
  public void put() {
    Group ns = new Route.Group("/ns");
    ns.put((req, rsp, chain) -> {
    });
    ns.put((req, rsp) -> {
    });
    ns.put(req -> "X");
    ns.put(() -> "X");

    assertEquals(4, ns.routes().size());
    matches(ns.routes(), "PUT", "/ns");
    noMatches(ns.routes(), "GET", "/ns");
  }

  @Test
  public void putWithPath() {
    Group ns = new Route.Group("/ns");
    ns.put("/p", (req, rsp, chain) -> {
    });
    ns.put("/p", (req, rsp) -> {
    });
    ns.put("/p", req -> "X");
    ns.put("/p", () -> "X");

    assertEquals(4, ns.routes().size());
    matches(ns.routes(), "PUT", "/ns/p");
    noMatches(ns.routes(), "GET", "/ns");
  }

  @Test
  public void delete() {
    Group ns = new Route.Group("/ns");
    ns.delete((req, rsp, chain) -> {
    });
    ns.delete((req, rsp) -> {
    });
    ns.delete(req -> "X");
    ns.delete(() -> "X");

    assertEquals(4, ns.routes().size());
    matches(ns.routes(), "DELETE", "/ns");
    noMatches(ns.routes(), "GET", "/ns");
  }

  @Test
  public void deleteWithPath() {
    Group ns = new Route.Group("/ns");
    ns.delete("/d", (req, rsp, chain) -> {
    });
    ns.delete("/d", (req, rsp) -> {
    });
    ns.delete("/d", req -> "X");
    ns.delete("/d", () -> "X");

    assertEquals(4, ns.routes().size());
    matches(ns.routes(), "DELETE", "/ns/d");
    noMatches(ns.routes(), "GET", "/ns");
  }

  @Test
  public void patch() {
    Group ns = new Route.Group("/ns");
    ns.patch((req, rsp, chain) -> {
    });
    ns.patch((req, rsp) -> {
    });
    ns.patch(req -> "X");
    ns.patch(() -> "X");

    assertEquals(4, ns.routes().size());
    matches(ns.routes(), "patch", "/ns");
    noMatches(ns.routes(), "GET", "/ns");
  }

  @Test
  public void patchWithPath() {
    Group ns = new Route.Group("/ns");
    ns.patch("/p", (req, rsp, chain) -> {
    });
    ns.patch("/p", (req, rsp) -> {
    });
    ns.patch("/p", req -> "X");
    ns.patch("/p", () -> "X");

    assertEquals(4, ns.routes().size());
    matches(ns.routes(), "patch", "/ns/p");
    noMatches(ns.routes(), "GET", "/ns");
  }

  @Test
  public void name() {
    Group ns = new Route.Group("/ns");
    ns.patch("/p", (req, rsp, chain) -> {
    }).name("x");

    assertEquals("/x", ns.routes().iterator().next().name());
  }

  @Test
  public void namens() {
    Group ns = new Route.Group("/ns", "/prefix");
    ns.patch("/p", (req, rsp, chain) -> {
    }).name("x");

    assertEquals("/prefix/x", ns.routes().iterator().next().name());
  }

  @Test
  public void consumes() {
    Group ns = new Route.Group("/ns");
    ns.patch("/p", (req, rsp, chain) -> {
    }).consumes("json");

    assertEquals("application/json", ns.routes().iterator().next().consumes().iterator().next()
        .name());
  }

  @Test
  public void consumesType() {
    Group ns = new Route.Group("/ns");
    ns.patch("/p", (req, rsp, chain) -> {
    }).consumes(MediaType.json);

    assertEquals(MediaType.json, ns.routes().iterator().next().consumes().iterator().next());
  }

  @Test
  public void produces() {
    Group ns = new Route.Group("/ns");
    ns.patch("/p", (req, rsp, chain) -> {
    }).produces("json");

    assertEquals("application/json", ns.routes().iterator().next().produces().iterator().next()
        .name());
  }

  @Test
  public void producesType() {
    Group ns = new Route.Group("/ns");
    ns.patch("/p", (req, rsp, chain) -> {
    }).produces(MediaType.json);

    assertEquals(MediaType.json, ns.routes().iterator().next().produces().iterator().next());
  }

  @Test
  public void renderer() {
    Group ns = new Route.Group("/ns");
    ns.patch("/p", (req, rsp, chain) -> {
    }).renderer("json");

    assertEquals("json", ns.routes().iterator().next().attr("renderer").get());
  }

  @Test
  public void attr() {
    Group ns = new Route.Group("/ns");
    ns.patch("/p", (req, rsp, chain) -> {
    }).attr("foo", "bar");

    assertEquals("bar", ns.routes().iterator().next().attr("foo").get());
  }

  private void matches(final List<Route.Definition> routes, final String method,
      final String pattern) {
    for (Route.Definition r : routes) {
      assertTrue(
          r.matches(method.toUpperCase(), pattern, MediaType.all, MediaType.ALL).isPresent());
    }
  }

  private void noMatches(final List<Route.Definition> routes, final String method,
      final String pattern) {
    for (Route.Definition r : routes) {
      assertFalse(
          r.matches(method.toUpperCase(), pattern, MediaType.all, MediaType.ALL).isPresent());
    }
  }

}
