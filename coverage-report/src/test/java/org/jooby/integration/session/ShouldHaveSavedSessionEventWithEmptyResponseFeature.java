package org.jooby.integration.session;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CountDownLatch;

import org.jooby.Session;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ShouldHaveSavedSessionEventWithEmptyResponseFeature extends ServerFeature {

  private static final CountDownLatch latch = new CountDownLatch(1);

  {
    use(new Session.MemoryStore() {

      @Override
      public void create(final Session session) {
        super.create(session);
        latch.countDown();
      }
    });

    get("/shouldHaveSavedSessionEvenWithEmptyResponse", (req, rsp) -> {
      req.session().set("k1", "v1");
      rsp.status(200);
    });
  }

  @Test
  public void shouldHaveSavedSessionEvenWithEmptyResponse() throws Exception {
    request()
        .get("/shouldHaveSavedSessionEvenWithEmptyResponse")
        .expect(200)
        .header("Set-Cookie", setCookie -> assertNotNull(setCookie));

    latch.await();
  }

}
