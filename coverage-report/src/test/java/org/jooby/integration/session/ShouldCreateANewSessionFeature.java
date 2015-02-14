package org.jooby.integration.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.atomic.AtomicReference;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ShouldCreateANewSessionFeature extends ServerFeature {

  private static final AtomicReference<String> sessionId = new AtomicReference<>();
  {
    get("/shouldCreateANewSession", req -> {
      sessionId.set(req.session().id());
      return sessionId.get();
    });
  }

  @Test
  public void shouldCreateANewSession() throws Exception {
    request()
        .get("/shouldCreateANewSession")
        .expect(sid -> assertEquals(sid, sessionId.get()))
        .header("Set-Cookie", setCookie -> assertNotNull(setCookie));
  }

}
