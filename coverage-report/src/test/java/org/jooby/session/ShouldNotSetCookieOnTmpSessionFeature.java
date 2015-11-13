package org.jooby.session;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;

import org.jooby.Session;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ShouldNotSetCookieOnTmpSessionFeature extends ServerFeature {

  private static final CountDownLatch latch = new CountDownLatch(1);

  {
    session(new Session.Mem() {
      @Override
      public void delete(final String id) {
        super.delete(id);
        latch.countDown();
      }
    });

    get("/tmpsession", (req, rsp) -> {
      Session session = req.session();
      assertTrue(req.ifSession().isPresent());
      session.destroy();
      assertTrue(!req.ifSession().isPresent());
      rsp.send("done");
    });

  }

  @Test
  public void shouldNotSetCookieOnTmpSession() throws Exception {
    request()
      .get("/tmpsession")
      .expect("done")
      .header("Set-Cookie", (String) null);

    latch.await();
  }

}
