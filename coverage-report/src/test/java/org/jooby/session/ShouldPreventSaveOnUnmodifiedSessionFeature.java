package org.jooby.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jooby.Session;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ShouldPreventSaveOnUnmodifiedSessionFeature extends ServerFeature {

  private static CountDownLatch latch = null;

  {
    session(new Session.Mem() {
      @Override
      public void create(final Session session) {
        super.create(session);
        latch.countDown();
      }

      @Override
      public void save(final Session session) {
        super.save(session);
        latch.countDown();
        throw new IllegalStateException();
      }
    });

    get("/shouldPreventSaveOnUnmodifiedSession", req -> {
      Session session = req.session();
      session.set("k1", "v1");
      return session.get("k1").value();
    });

  }

  @Test
  public void shouldPreventSaveOnUnmodifiedSession() throws Exception {
    latch = new CountDownLatch(2);

    request()
        .get("/shouldPreventSaveOnUnmodifiedSession")
        .expect(200)
        .header("Set-Cookie", setCookie -> assertNotNull(setCookie));

    request()
        .get("/shouldPreventSaveOnUnmodifiedSession")
        .expect(200)
        .header("Set-Cookie", (String) null);

    request()
        .get("/shouldPreventSaveOnUnmodifiedSession")
        .expect(200)
        .header("Set-Cookie", (String) null);

    latch.await(1000, TimeUnit.MILLISECONDS);
    assertEquals(1, latch.getCount());
  }

}
