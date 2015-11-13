package org.jooby.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.jooby.Session;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.common.base.Splitter;

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

    session(new Session.Mem() {
      @Override
      public Session get(final Session.Builder builder) {
        assertEquals("678", builder.sessionId());
        Map<String, String> attrs = new LinkedHashMap<String, String>();
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
    }).cookie().maxAge(2);

    get("/restore", req -> {
      Session session = req.session();
      return "createdAt:" + session.createdAt() + "\n" +
          "accessedAt:" + session.accessedAt() + "\n" +
          "savedAt:" + session.savedAt() + "\n" +
          "expiryAt:" + session.expiryAt() + "\n" +
          "attributes:" + session.attributes();
    });

  }

  @Test
  public void shouldRestoreSessionFromCookieID() throws Exception {
    request()
        .get("/restore")
        .header("Cookie", "jooby.sid=678")
        .expect(rsp -> {
          Map<String, String> result = new HashMap<>();
          Splitter.on("\n").splitToList(rsp).forEach(line -> {
            String[] entry = line.split(":");
            result.put(entry[0], entry[1]);
          });
          assertEquals(createdAt.get(), Long.parseLong(result.remove("createdAt")));
          assertEquals(lastSaved.get(), Long.parseLong(result.remove("savedAt")));
          assertEquals(lastAccessed.get(), Long.parseLong(result.remove("accessedAt")));
          assertEquals(expiryAt.get(), Long.parseLong(result.remove("expiryAt")));
          assertEquals("{k1=v1.1}", result.remove("attributes"));
          assertTrue(result.toString(), result.isEmpty());
        })
        .expect(200);

    latch.await();
  }

}
