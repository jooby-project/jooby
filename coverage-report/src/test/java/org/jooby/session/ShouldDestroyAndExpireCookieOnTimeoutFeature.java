package org.jooby.session;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.jooby.Session;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class ShouldDestroyAndExpireCookieOnTimeoutFeature extends ServerFeature {

  {
    use(ConfigFactory.empty().withValue("application.session.cookie.maxAge", ConfigValueFactory.fromAnyRef("1s")));

    session(new Session.Mem());

    get("/session", req -> {
      return req.session().id();
    });
  }

  @Test
  public void shouldDestroyAndExpireCookieOnTimeout() throws Exception {
    request()
        .get("/session")
        .expect(200)
        .header("Set-Cookie", setCookie -> assertNotNull(setCookie))
        .expect(sessionId1 -> {
          Thread.sleep(1200L);

          request()
              .get("/session")
              .expect(200)
              .header("Set-Cookie", setCookie -> assertNotNull(setCookie))
              .expect(sessionId2 -> {

                assertNotEquals(sessionId1, sessionId2);
              });
        });
  }

}
