package org.jooby;

import java.util.concurrent.atomic.AtomicInteger;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class LiftMixedFeature extends ServerFeature {
  {

    use("*", (req, rsp, chain) -> {
      if (req.param("admin").booleanValue(false)) {
        chain.next("/admin", req, rsp);
      } else {
        chain.next("/frontend", req, rsp);
      }
    });

    AtomicInteger counter = new AtomicInteger(0);

    get("/", (req, rsp) -> {
      counter.set(0);
      counter.incrementAndGet();
    }).name("admin/home");

    get("/", (req, rsp) -> {
      counter.set(0);
      counter.incrementAndGet();
    }).name("frontend/home");

    get("/", () -> "f" + counter.get()).name("frontend/home");

    get("/", (req, rsp) -> {
      counter.incrementAndGet();
    }).name("admin/home");

    get("/", () -> "a" + counter.get()).name("admin/home");

    get("/:id", req -> "a" + req.param("id").value()).name("admin/1");

    get("/:id", req -> "f" + req.param("id").value()).name("frontend/1");

  }

  @Test
  public void shouldPickAdminRoutes() throws Exception {
    request()
        .get("/?admin=true")
        .expect("a2");

    request()
        .get("/1?admin=true")
        .expect("a1");

    request()
        .get("/1/x?admin=true")
        .expect(404);
  }

  @Test
  public void shouldPickFrontendRoutes() throws Exception {
    request()
        .get("/?admin=false")
        .expect("f1");

    request()
        .get("/1")
        .expect("f1");

    request()
        .get("/1/x")
        .expect(404);
  }
}
