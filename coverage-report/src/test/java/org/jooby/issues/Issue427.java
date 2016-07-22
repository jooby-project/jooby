package org.jooby.issues;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jooby.Results;
import org.jooby.Session;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue427 extends ServerFeature {

  {
    use(ConfigFactory.empty().withValue("application.secret",
        ConfigValueFactory.fromAnyRef("1234Querty")));

    cookieSession();

    AtomicInteger inc = new AtomicInteger();

    get("/427", req -> {
      Session session = req.session();
      session.set("foo", inc.incrementAndGet());
      Map<String, Object> hash = new LinkedHashMap<>(session.attributes());
      hash.put("id", session.id());
      hash.put("createdAt", session.createdAt());
      hash.put("accessedAt", session.accessedAt());
      hash.put("savedAt", session.savedAt());
      hash.put("expireAt", session.expiryAt());
      hash.put("toString", session.toString());
      return hash;
    });

    get("/427/destroy", req -> {
      req.session().destroy();
      return Results.ok();
    });

    get("/427/:name", req -> {
      return req.session().get(req.param("name").value()).value();
    });
  }

  @Test
  public void sessionData() throws Exception {
    long maxAge = System.currentTimeMillis() + 60 * 1000;
    DateTimeFormatter.ofPattern("E, dd-MMM-yyyy HH:mm")
        .withZone(ZoneId.of("GMT"))
        .withLocale(Locale.ENGLISH);
    Instant.ofEpochMilli(maxAge);

    request()
        .get("/427")
        .expect("{foo=1, id=cookieSession, createdAt=-1, accessedAt=-1, savedAt=-1, expireAt=-1, toString=cookieSession}")
        .header("Set-Cookie",
            "jooby.sid=Kq4J4jA6mChDXuIQxxrEibEzA09szjJ89IB3UQuWwAM|foo=1;Version=1;Path=/;HttpOnly");

    request()
        .get("/427/foo")
        .expect("1");

    request()
        .get("/427")
        .expect(200)
        .header("Set-Cookie",
            "jooby.sid=jajRvd/dtopEAwPK/vC59J3V5cACzfbnYMPfICaC4f8|foo=2;Version=1;Path=/;HttpOnly");

    request()
        .get("/427/foo")
        .expect("2");

    request()
        .get("/427/destroy")
        .expect(200)
        .header("Set-Cookie",
            "jooby.sid=;Version=1;Max-Age=0;Expires=Thu, 01-Jan-1970 00:00:00 GMT");
  }

}
