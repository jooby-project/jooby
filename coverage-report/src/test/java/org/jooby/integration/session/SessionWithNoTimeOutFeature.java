package org.jooby.integration.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.jooby.Session;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class SessionWithNoTimeOutFeature extends ServerFeature {

  {
    use(new Session.MemoryStore()).timeout(-1);

    get("/session", (req, rsp) -> {
      Session session = req.session();
      assertEquals(-1, session.expiryAt());
      rsp.send(session.expiryAt());
    });

  }

  @Test
  public void sessionWithNoTimeOff() throws Exception {
    request()
        .get("/session")
        .expect("-1")
        .header("Set-Cookie", setCookie -> {
          assertNotNull(setCookie);
          request()
              .get("/session")
              .expect("-1")
              .header("Set-Cookie", (String) null);
        });
  }

}
