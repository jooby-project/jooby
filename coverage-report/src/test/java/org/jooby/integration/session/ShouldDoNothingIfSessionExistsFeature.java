package org.jooby.integration.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ShouldDoNothingIfSessionExistsFeature extends ServerFeature {

  {
    get("/create", req -> req.session().id());

    get("/get", req -> req.session().id());
  }

  @Test
  public void shouldDoNothingIfSessionExists() throws Exception {

    request()
        .get("/create")
        .expect(200)
        .header("Set-Cookie", setCookie -> assertNotNull(setCookie))
        .expect(sessionId1 -> {
          request()
              .get("/get")
              .expect(200)
              .header("Set-Cookie", (String) null)
              .expect(sessionId2 -> assertEquals(sessionId1, sessionId2));
        });
  }

}
