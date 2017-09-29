package org.jooby.pac4j2;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class BasicAuthFeature extends ServerFeature {

  {

    use(new Auth().basic());

    get("/auth/basic", req -> req.path());
  }

  @Test
  public void auth() throws Exception {
    request()
        .basic("test", "test")
        .get("/auth/basic")
        .expect("/auth/basic");
  }

  @Test
  public void unauthorizedAjax() throws Exception {
    request()
        .get("/auth/basic")
        .header("X-Requested-With", "XMLHttpRequest")
        .expect(401);
  }

  @Test
  public void unauthorized() throws Exception {
    request()
        .get("/auth/basic")
        .expect(401);

  }

  @Test
  public void badCredentials() throws Exception {
    request()
        .basic("test", "")
        .get("/auth/basic")
        .expect(401);
  }

}
