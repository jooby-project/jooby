package org.jooby.session;

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.jooby.Session;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class SessionRestoreFeature extends ServerFeature {

  private static final CountDownLatch latch = new CountDownLatch(1);

  private static final AtomicLong createdAt = new AtomicLong();

  private static final AtomicLong lastAccessed = new AtomicLong();

  private static final AtomicLong lastSaved = new AtomicLong();

  private static final AtomicLong expiryAt = new AtomicLong();

  {
    createdAt.set(System.currentTimeMillis() - 1000);
    lastAccessed.set(System.currentTimeMillis() - 200);
    lastSaved.set(lastAccessed.get());
    expiryAt.set(lastAccessed.get() + 2000);
    session(new Session.MemoryStore() {
      @Override
      public Session get(final Session.Builder builder) {
        assertEquals("678", builder.sessionId());
        Map<String, Object> attrs = new LinkedHashMap<String, Object>();
        attrs.put("k1", "v1.1");
        Session session = builder
            .accessedAt(lastAccessed.get())
            .createdAt(createdAt.get())
            .savedAt(lastSaved.get())
            .set("k1", "v1")
            .set(attrs)
            .build();
        latch.countDown();
        return session;
      }

    }).timeout(2);

    get("/restore", (req, rsp) -> {
      Session session = req.session();
      rsp.send(session.createdAt() + "; " + session.accessedAt() + "; " + session.savedAt()
          + "; " + session.expiryAt()
          + "; " + session.attributes());
    });

  }

  @Test
  public void shouldRestoreSessionFromCookieID() throws Exception {
    request()
        .get("/restore")
        .header("Cookie", "jooby.sid=678")
        .expect(createdAt + "; " + lastAccessed + "; " + lastSaved + "; " + expiryAt
            + "; {k1=v1.1}")
        .expect(200);

    latch.await();
  }

}
