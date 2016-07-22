package org.jooby.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.jooby.Results;
import org.jooby.Session;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue427b extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("application.secret", ConfigValueFactory.fromAnyRef("1234Querty"))
        .withValue("session.cookie.maxAge", ConfigValueFactory.fromAnyRef(30)));

    cookieSession();

    AtomicInteger inc = new AtomicInteger();

    get("/427", req -> {
      Session session = req.session();
      session.set("foo", inc.incrementAndGet());
      return Results.ok();
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
  public void sessionDataWithMaxAge() throws Exception {
    long maxAge = System.currentTimeMillis() + 30 * 1000;
    // remove seconds to make sure test always work
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E, dd-MMM-yyyy HH:mm")
        .withZone(ZoneId.of("GMT"))
        .withLocale(Locale.ENGLISH);
    Instant instant = Instant.ofEpochMilli(maxAge);

    Set<String> values = new HashSet<>();
    request()
        .get("/427")
        .expect(200)
        .header("Set-Cookie", value -> {
          values.add(value);
          List<String> setCookie = Lists.newArrayList(
              Splitter.onPattern(";\\s*")
                  .splitToList(value));

          assertTrue(setCookie.remove(0).startsWith("jooby.sid"));
          assertTrue(setCookie.remove("Path=/") || setCookie.remove("Path=\"/\""));
          assertTrue(setCookie.remove("HttpOnly") || setCookie.remove("HTTPOnly"));
          assertTrue(value, setCookie.remove("Max-Age=30"));
          assertTrue(value, setCookie.remove("Version=1"));
          assertTrue(setCookie.remove(0).startsWith(
              "Expires=" + formatter.format(instant).replace("GMT", "")));
        });

    Thread.sleep(1000L);
    request()
        .get("/427/foo")
        .expect("1")
        .header("Set-Cookie", value -> {
          values.add(value);
          List<String> setCookie = Lists.newArrayList(
              Splitter.onPattern(";\\s*")
                  .splitToList(value));

          assertTrue(setCookie.remove(0).startsWith("jooby.sid"));
          assertTrue(setCookie.remove("Path=/") || setCookie.remove("Path=\"/\""));
          assertTrue(setCookie.remove("HttpOnly") || setCookie.remove("HTTPOnly"));
          assertTrue(value, setCookie.remove("Max-Age=30"));
          assertTrue(value, setCookie.remove("Version=1"));
          assertTrue(setCookie.remove(0).startsWith(
              "Expires=" + formatter.format(instant).replace("GMT", "")));
        });

    assertEquals(2, values.size());
  }

}
