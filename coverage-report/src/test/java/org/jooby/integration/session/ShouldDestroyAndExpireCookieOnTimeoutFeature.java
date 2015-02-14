package org.jooby.integration.session;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CountDownLatch;

import org.jooby.Session;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ShouldDestroyAndExpireCookieOnTimeoutFeature extends ServerFeature {

  private static final CountDownLatch latch = new CountDownLatch(1);

  {
    use(new Session.MemoryStore() {
      @Override
      public void delete(final String id) {
        super.delete(id);
        latch.countDown();
      }

    }).timeout(1);

    get("/session", (req, rsp) -> {
      rsp.send(req.session().id());
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
                latch.await();

                assertNotEquals(sessionId1, sessionId2);
              });
        });
  }

}
