package org.jooby.session;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;

import org.jooby.Session;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class SessionWithMaxAgeShouldAlwaysSendHeaderFeature extends ServerFeature {

  private static AtomicReference<String> ID = new AtomicReference<String>();

  {
    session(new Session.Mem() {
      @Override
      public String generateID() {
        String id = super.generateID();
        ID.set(id);
        return id;
      }
    }).cookie().maxAge(2);

    get("/session", req -> {
      Session session = req.session();
      return session.id();
    });

  }

  @Test
  public void shouldRestoreSessionFromCookieID() throws Exception {
    ID.set(null);

    long maxAge = System.currentTimeMillis() + 2 * 1000;
    // remove seconds to make sure test always work
    Instant instant = Instant.ofEpochMilli(maxAge);

    request()
        .get("/session")
        .expect(200)
        .header("Set-Cookie", setCookie -> {
          assertTrue(setCookie.startsWith(sessionId(ID.get(), instant)));
        });

    String existingID = ID.get();

    request()
        .get("/session")
        .expect(200)
        .header("Set-Cookie", setCookie -> {
          assertTrue(setCookie.startsWith(sessionId(ID.get(), instant)));
        });

    request()
        .get("/session")
        .expect(200)
        .header("Set-Cookie", setCookie -> {
          assertTrue(setCookie.startsWith(sessionId(ID.get(), instant)));
        });

    // reset cookies
    request()
        .resetCookies()
        .get("/session")
        .expect(200)
        .header("Set-Cookie", setCookie -> {
          assertNotEquals(ID.get(), existingID);
          assertTrue(setCookie.startsWith(sessionId(ID.get(), instant)));
        });
  }

  private String sessionId(final String id, final Instant instant) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E, dd-MMM-yyyy HH:mm")
        .withZone(ZoneId.of("GMT"));
    return "jooby.sid=" + id + ";Version=1;Path=/;HttpOnly;Max-Age=2;Expires="
        + formatter.format(instant);
  }

}
