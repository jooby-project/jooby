package org.jooby.session;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import org.jooby.Session;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.common.collect.Sets;

public class SessionDefMaxAgeShouldSendHeaderJustOnceFeature extends ServerFeature {

  private static AtomicReference<String> ID = new AtomicReference<String>();

  {
    session(new Session.Mem() {
      @Override
      public String generateID() {
        String id = super.generateID();
        ID.set(id);
        return id;
      }
    }).cookie().maxAge(-1);

    get("/session", req -> {
      Session session = req.session();
      return session.id();
    });

  }

  @Test
  public void shouldRestoreSessionFromCookieID() throws Exception {
    ID.set(null);

    request()
        .get("/session")
        .expect(200)
        .header("Set-Cookie", h -> {
          assertTrue(h, Sets.newHashSet(
              "jooby.sid=" + ID.get() + "; Version=1; Path=/; HttpOnly",
              "jooby.sid=" + ID.get() + ";Version=1;Path=/;HttpOnly",
              "jooby.sid=" + ID.get() + "; Path=\"/\"; HTTPOnly; Version=1"
              ).contains(h));
        });

    String existingID = ID.get();

    request()
        .get("/session")
        .expect(200)
        .header("Set-Cookie", (String) null);

    request()
        .get("/session")
        .expect(200)
        .header("Set-Cookie", (String) null);

    // browser close
    request()
        .resetCookies()
        .get("/session")
        .expect(200)
        .header("Set-Cookie", h -> {
          assertNotEquals(ID.get(), existingID);
          assertTrue(h, Sets.newHashSet(
              "jooby.sid=" + ID.get() + "; Version=1; Path=/; HttpOnly",
              "jooby.sid=" + ID.get() + ";Version=1;Path=/;HttpOnly",
              "jooby.sid=" + ID.get() + "; Path=\"/\"; HTTPOnly; Version=1"
              ).contains(h));
        });
  }

}
