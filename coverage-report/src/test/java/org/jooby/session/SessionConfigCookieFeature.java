package org.jooby.session;

import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.jooby.Session;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class SessionConfigCookieFeature extends ServerFeature {

  {
    use(ConfigFactory
        .empty()
        .withValue("application.secret", ConfigValueFactory.fromAnyRef("fixed"))
        .withValue("session.cookie.name", ConfigValueFactory.fromAnyRef("custom.sid"))
        .withValue("session.cookie.path", ConfigValueFactory.fromAnyRef("/session"))
        .withValue("session.cookie.comment",
            ConfigValueFactory.fromAnyRef("jooby cookie"))
        .withValue("session.cookie.domain", ConfigValueFactory.fromAnyRef("localhost"))
        .withValue("session.cookie.maxAge", ConfigValueFactory.fromAnyRef(60))
        .withValue("session.cookie.httpOnly", ConfigValueFactory.fromAnyRef(true))
        .withValue("session.cookie.secure", ConfigValueFactory.fromAnyRef(false)));

    session(new Session.Mem());

    get("/session", (req, rsp) -> {
      rsp.send(req.session().id());
    });

  }

  @Test
  public void cookieConfig() throws Exception {
    long maxAge = System.currentTimeMillis() + 60 * 1000;
    // remove seconds to make sure test always work
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E, dd-MMM-yyyy HH:mm")
        .withZone(ZoneId.of("GMT"));
    Instant instant = Instant.ofEpochMilli(maxAge);

    request()
        .get("/session")
        .expect(200)
        .header("Set-Cookie", value -> {
          List<String> setCookie = Lists.newArrayList(
              Splitter.onPattern(";\\s*")
                  .splitToList(value)
              );

          assertTrue(setCookie.remove(0).startsWith("custom.sid"));
          assertTrue(setCookie.remove("Path=/session") || setCookie.remove("Path=\"/session\""));
          assertTrue(setCookie.remove("HttpOnly") || setCookie.remove("HTTPOnly"));
          assertTrue(value, setCookie.remove("Max-Age=60"));
          assertTrue(setCookie.remove("Domain=localhost"));
          assertTrue(value, setCookie.remove("Version=1"));
          assertTrue(setCookie.remove("Comment=\"jooby cookie\""));
          assertTrue(setCookie.remove(0).startsWith(
              "Expires=" + formatter.format(instant).replace("GMT", "")));
        });
  }

}
