package org.jooby.integration.session;

import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
        .withValue("application.session.cookie.name", ConfigValueFactory.fromAnyRef("custom.sid"))
        .withValue("application.session.cookie.path", ConfigValueFactory.fromAnyRef("/session"))
        .withValue("application.session.cookie.comment",
            ConfigValueFactory.fromAnyRef("jooby cookie"))
        .withValue("application.session.cookie.domain", ConfigValueFactory.fromAnyRef("localhost"))
        .withValue("application.session.cookie.maxAge", ConfigValueFactory.fromAnyRef(60))
        .withValue("application.session.cookie.httpOnly", ConfigValueFactory.fromAnyRef(true))
        .withValue("application.session.cookie.secure", ConfigValueFactory.fromAnyRef(false)));

    use(new Session.MemoryStore());

    get("/session", (req, rsp) -> {
      rsp.send(req.session().id());
    });

  }

  @Test
  public void cookieConfig() throws Exception {
    long maxAge = System.currentTimeMillis() + 60 * 1000;
    // remove seconds to make sure test always work
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd-MMM-yyyy HH:mm");
    Instant instant = Instant.ofEpochMilli(maxAge);
    OffsetDateTime utc = instant.atOffset(ZoneOffset.UTC);

    request()
        .get("/session")
        .expect(200)
        .header(
            "Set-Cookie",
            value -> {
              List<String> setCookie = Lists.newArrayList(
                  Splitter.onPattern(";\\s*")
                      .splitToList(value)
                  );

              assertTrue(setCookie.remove(0).startsWith("custom.sid"));
              assertTrue(setCookie.remove("Path=/session"));
              assertTrue(setCookie.remove("HttpOnly"));
              assertTrue(setCookie.remove("Max-Age=60"));
              assertTrue(setCookie.remove("Domain=localhost"));
              assertTrue(setCookie.remove("Version=1"));
              if (setCookie.size() > 0) {
                // Expires is optional on version=1?
                assertTrue(setCookie.remove(0).startsWith(
                    "Expires=" + formatter.format(utc).replace("GMT", "")));
              }
            });
  }

}
