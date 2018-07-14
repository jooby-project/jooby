package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class LiftWithAppsFeature extends ServerFeature {
  public static class Admin extends Jooby {

    public Admin(final String prefix) {
      super(prefix);
    }

    {
      path("/", () -> {
          get("/", () -> "admin");
          get("/:id", req -> "a" + req.param("id").value());
      });
    }
  }

  public static class Frontend extends Jooby {

    public Frontend(final String prefix) {
      super(prefix);
    }

    {
      get("/", () -> "frontend");

      get("/:id", req -> "f" + req.param("id").value());
    }
  }

  {

    use("*", (req, rsp, chain) -> {
      if (req.param("admin").booleanValue(false)) {
        chain.next("/a", req, rsp);
      } else {
        chain.next("/f", req, rsp);
      }
    });

    use(new Admin("a"));

    use(new Frontend("f"));

  }

  @Test
  public void shouldPickAdminRoutes() throws Exception {
    request()
        .get("/?admin=true")
        .expect("admin");

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
        .expect("frontend");

    request()
        .get("/1")
        .expect("f1");

    request()
        .get("/1/x")
        .expect(404);
  }
}
