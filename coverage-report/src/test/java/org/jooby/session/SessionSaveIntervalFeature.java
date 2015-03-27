package org.jooby.session;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CountDownLatch;

import org.jooby.Session;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class SessionSaveIntervalFeature extends ServerFeature {

  private static final CountDownLatch saveCalls = new CountDownLatch(2);

  {
    session(new Session.Mem() {
      @Override
      public void save(final Session session) {
        saveCalls.countDown();
      }
    }).saveInterval(1);

    get("/session", (req, rsp) -> {
      rsp.send(req.session().id());
    });

  }

  @Test
  public void sessionMustBeSavedOnSaveInterval() throws Exception {
    request()
        .get("/session")
        .expect(200)
        .header("Set-Cookie", setCookie -> assertNotNull(setCookie));

    request()
        .get("/session")
        .expect(200)
        .header("Set-Cookie", (String) null);

    request()
        .get("/session")
        .expect(200)
        .header("Set-Cookie", (String) null);

    Thread.sleep(1200L);

    request()
        .get("/session")
        .expect(200)
        .header("Set-Cookie", (String) null);

    saveCalls.await();
  }

}
