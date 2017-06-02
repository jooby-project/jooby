package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class LiftFeature extends ServerFeature {

  {
    use("*", (req, rsp, chain) -> {
      if (req.param("admin").booleanValue(false)) {
        chain.next("/admin", req, rsp);
      } else {
        chain.next("/normal", req, rsp);
      }
    });

    get("/", () -> "Hello admin").name("admin");

    get("/", () -> "Hello user").name("normal");
  }

  @Test
  public void shouldPickAdminRoutes() throws Exception {
    request()
        .get("/?admin=true")
        .expect("Hello admin");

    request()
        .get("/1/x?admin=true")
        .expect(404);
  }

  @Test
  public void shouldPickFrontendRoutes() throws Exception {
    request()
        .get("/?admin=false")
        .expect("Hello user");

    request()
        .get("/1/x")
        .expect(404);
  }
}
