package org.jooby.session;

import static org.junit.Assert.assertNotNull;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ShouldCreateMultipleSessionsFeature extends ServerFeature {

  {
    get("/shouldCreateMutipleSessions", req -> {
      return req.session().get("count").map(c -> "updated").orElse("created");
    });
  }

  @Test
  public void shouldCreateMutipleSessions() throws Exception {
    request()
        .get("/shouldCreateMutipleSessions")
        .expect("created")
        .header("Set-Cookie", setCookie -> assertNotNull(setCookie));

    request()
        .resetCookies()
        .get("/shouldCreateMutipleSessions")
        .expect("created")
        .header("Set-Cookie", setCookie -> assertNotNull(setCookie));
  }

}
