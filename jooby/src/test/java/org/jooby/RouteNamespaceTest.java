package org.jooby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jooby.Route.Namespace;
import org.junit.Test;

public class RouteNamespaceTest {

  @Test
  public void all() {
    Namespace ns = new Route.Namespace("/ns");
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
    Namespace ns = new Route.Namespace("/ns");
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
    Namespace ns = new Route.Namespace("/ns");
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
    Namespace ns = new Route.Namespace("/ns");
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
    Namespace ns = new Route.Namespace("/ns");
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
    Namespace ns = new Route.Namespace("/ns");
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
    Namespace ns = new Route.Namespace("/ns");
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
    Namespace ns = new Route.Namespace("/ns");
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
    Namespace ns = new Route.Namespace("/ns");
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
    Namespace ns = new Route.Namespace("/ns");
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
    Namespace ns = new Route.Namespace("/ns");
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
    Namespace ns = new Route.Namespace("/ns");
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
    Namespace ns = new Route.Namespace("/ns");
    ns.patch("/p", (req, rsp, chain) -> {
    }).name("x");

    assertEquals("x", ns.routes().iterator().next().name());
  }

  @Test
  public void consumes() {
    Namespace ns = new Route.Namespace("/ns");
    ns.patch("/p", (req, rsp, chain) -> {
    }).consumes("json");

    assertEquals("application/json", ns.routes().iterator().next().consumes().iterator().next()
        .name());
  }

  @Test
  public void consumesType() {
    Namespace ns = new Route.Namespace("/ns");
    ns.patch("/p", (req, rsp, chain) -> {
    }).consumes(MediaType.json);

    assertEquals(MediaType.json, ns.routes().iterator().next().consumes().iterator().next());
  }

  @Test
  public void produces() {
    Namespace ns = new Route.Namespace("/ns");
    ns.patch("/p", (req, rsp, chain) -> {
    }).produces("json");

    assertEquals("application/json", ns.routes().iterator().next().produces().iterator().next()
        .name());
  }

  @Test
  public void producesType() {
    Namespace ns = new Route.Namespace("/ns");
    ns.patch("/p", (req, rsp, chain) -> {
    }).produces(MediaType.json);

    assertEquals(MediaType.json, ns.routes().iterator().next().produces().iterator().next());
  }

  private void matches(final List<Route.Definition> routes, final String method,
      final String pattern) {
    for (Route.Definition r : routes) {
      assertTrue(r.matches(method, pattern, MediaType.all, MediaType.ALL).isPresent());
    }
  }

  private void noMatches(final List<Route.Definition> routes, final String method,
      final String pattern) {
    for (Route.Definition r : routes) {
      assertFalse(r.matches(method, pattern, MediaType.all, MediaType.ALL).isPresent());
    }
  }

}
