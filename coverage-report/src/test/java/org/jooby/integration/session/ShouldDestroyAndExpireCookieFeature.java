package org.jooby.integration.session;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;

import org.jooby.Session;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ShouldDestroyAndExpireCookieFeature extends ServerFeature {

  private static final CountDownLatch latch = new CountDownLatch(1);

  {
    use(new Session.MemoryStore() {
      @Override
      public void delete(final String id) {
        super.delete(id);
        latch.countDown();
      }

    });

    get("/session", (req, rsp) -> {
      rsp.send(req.session().id());
    });

    get("/invalidate", (req, rsp) -> {
      Session session = req.session();
      assertTrue(req.ifSession().isPresent());
      session.destroy();
      assertTrue(!req.ifSession().isPresent());
      rsp.send("done");
    });

  }

  @Test
  public void shouldDestroyAndExpireCookie() throws Exception {
    request()
        .get("/session")
        .expect(200)
        .header("Set-Cookie", setCookie ->
            assertNotNull(setCookie)
        );

    request()
        .get("/invalidate")
        .expect(200)
        .header("Set-Cookie", setCookie -> {
          assertNotNull(setCookie);
          assertTrue(setCookie.endsWith("01-Jan-1970 00:00:00 GMT"));
        });

    latch.await();
  }

}
