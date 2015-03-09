package org.jooby.session;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.atomic.AtomicInteger;

import org.jooby.Session;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ShouldLoadSessionFromCookieIDFeature extends ServerFeature {

  {
    AtomicInteger cookieIDCounter = new AtomicInteger(0);
    get("/shouldLoadSessionFromCookieID", req -> {
      Session session = req.session();
      if (!session.get("count").isPresent()) {
        session.set("count", cookieIDCounter.incrementAndGet());
      }
      return session.get("count").get();
    });
  }

  @Test
  public void shouldLoadSessionFromCookieID() throws Exception {
    request()
        .get("/shouldLoadSessionFromCookieID")
        .expect(200)
        .header("Set-Cookie", setCookie -> assertNotNull(setCookie))
        .expect(count ->
            request()
                .get("/shouldLoadSessionFromCookieID")
                .expect(count)
                .header("Set-Cookie", (String) null)
        );
  }

}
