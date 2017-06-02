package org.jooby.pac4j;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class LogoutWithCustomRedirectToFeature extends ServerFeature {

  {

    get("/x", req -> req.path());

    use(new Auth().basic().logout("/out", "/x"));

    get("/auth/basic", req -> req.path());
  }

  @Test
  public void auth() throws Exception {
    request()
        .basic("test", "test")
        .get("/auth/basic")
        .expect("/auth/basic");

    request()
        .get("/out")
        .expect("/x");
  }

}
