package org.jooby.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.Session;
import org.jooby.integration.FilterFeature.HttpResponseValidator;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class SessionFeature extends ServerFeature {

  {

    use(ConfigFactory.empty().withValue("application.secret",
        ConfigValueFactory.fromAnyRef("fixed")));

    use(new Session.Store() {
      @Override
      public void save(final Session session, final SaveReason reason) {
        assertNotNull(session);
        session.set("saves", ((int) session.get("saves").orElse(0)) + 1);
      }

      @Override
      public Session get(final Session.Builder builder) {
        return null;
      }

      @Override
      public void delete(final String id) {
      }

      @Override
      public String generateID(final long seed) {
        return "1234";
      }
    }).timeout(3);

    get("/no-session", (req, rsp) -> {
      rsp.send(req.ifSession());
    });

    get("/session", (req, rsp) -> {
      rsp.send(req.session().get("saves").orElse(0));
    });

    get("/session/0", (req, rsp) -> {
      rsp.send(req.session().createdAt());
    });

    get("/session/1", (req, rsp) -> {
      rsp.send(req.session().accessedAt());
    });

    get("/session/str", (req, rsp) -> {
      rsp.send(req.session());
    });
  }

  @Test
  public void noSession() throws Exception {
    assertEquals("Optional.empty", execute(GET(uri("no-session")), (response) -> {
      assertEquals(200, response.getStatusLine().getStatusCode());
      assertEquals(null, response.getFirstHeader("Set-Cookie"));
    }));
  }

  @Test
  public void toStr() throws Exception {
    assertNotNull(execute(GET(uri("session", "str")), (response) -> {
      assertEquals(200, response.getStatusLine().getStatusCode());
    }));
  }

  @Test
  public void session() throws Exception {
    String cookieId = "jooby.sid=1234|YCoA3Xy3SpWxF95bTC+lVLg/GtTCO8YkKFkTeQ15v3E;Path=/;Secure;HttpOnly";
    assertEquals(
        "0",
        execute(
            GET(uri("session")),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
              assertEquals(cookieId, response.getFirstHeader("Set-Cookie").getValue());
            }));

    assertEquals(
        "1",
        execute(
            GET(uri("session")).addHeader("Cookie", cookieId),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
              assertNull(response.getFirstHeader("Set-Cookie"));
            }));

  }

  @Test
  public void newSessions() throws Exception {
    String cookieId = "jooby.sid=1234|YCoA3Xy3SpWxF95bTC+lVLg/GtTCO8YkKFkTeQ15v3E;Path=/;Secure;HttpOnly";
    assertEquals(
        "0",
        execute(
            GET(uri("session")),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
              assertEquals(cookieId, response.getFirstHeader("Set-Cookie").getValue());
            }));

    assertEquals(
        "0",
        execute(
            GET(uri("session")),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
              assertEquals(cookieId, response.getFirstHeader("Set-Cookie").getValue());
            }));

    assertEquals(
        "0",
        execute(
            GET(uri("session")),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
              assertEquals(cookieId, response.getFirstHeader("Set-Cookie").getValue());
            }));
  }

  @Test
  public void tamperedSession() throws Exception {
    String cookieId = "jooby.sid=1234|YCoA3Xy3SpWxF95bTC+lVLg/GtTCO8YkKFkTeQ15v3E;Path=/;Secure;HttpOnly";
    String tamperedId1 = "jooby.sid=6590|anN8BeWjnfVFT4P/FGkN7YbYAPhfXvTCx7P9CBrPa/s;Path=/;Secure;HttpOnly";
    String tamperedId2 = "jooby.sid=1234|anN8BeWjnfVFT4P/FGkN7YxbYAPhfXvTCx7P9BrPa/s;Path=/;Secure;HttpOnly";
    String brokenId = "jooby.sid=1234;Path=/;Secure;HttpOnly";
    assertEquals(
        "0",
        execute(
            GET(uri("session")),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
              assertEquals(cookieId, response.getFirstHeader("Set-Cookie").getValue());
            }));

    assertEquals(
        "0",
        execute(
            GET(uri("session")).addHeader("Cookie", tamperedId1),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
              assertEquals(cookieId, response.getFirstHeader("Set-Cookie").getValue());
            }));

    assertEquals(
        "0",
        execute(
            GET(uri("session")).addHeader("Cookie", tamperedId2),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
              assertEquals(cookieId, response.getFirstHeader("Set-Cookie").getValue());
            }));

    assertEquals(
        "0",
        execute(
            GET(uri("session")).addHeader("Cookie", brokenId),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
              assertEquals(cookieId, response.getFirstHeader("Set-Cookie").getValue());
            }));
  }

  @Test
  public void time() throws Exception {
    String cookieId = "jooby.sid=1234|YCoA3Xy3SpWxF95bTC+lVLg/GtTCO8YkKFkTeQ15v3E;Path=/;Secure;HttpOnly";
    long createdAt = Long.parseLong(execute(
        GET(uri("session/0")),
        (response) -> {
          assertEquals(200, response.getStatusLine().getStatusCode());
          assertEquals(cookieId, response.getFirstHeader("Set-Cookie").getValue());
        }));
    assertTrue(createdAt > 0);

    long access1 = Long.parseLong(execute(
        GET(uri("session/1")).addHeader("Cookie", cookieId),
        (response) -> {
          assertEquals(200, response.getStatusLine().getStatusCode());
        }));

    assertTrue(access1 > createdAt);
  }

  @Test
  public void timeout() throws Exception {
    String cookieId = "jooby.sid=1234|YCoA3Xy3SpWxF95bTC+lVLg/GtTCO8YkKFkTeQ15v3E;Path=/;Secure;HttpOnly";
    assertEquals(
        "0",
        execute(
            GET(uri("session")),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
              assertEquals(cookieId, response.getFirstHeader("Set-Cookie").getValue());
            }));

    Thread.sleep(3000L);
    assertEquals(
        "0",
        execute(
            GET(uri("session")).addHeader("Cookie", cookieId),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
              assertEquals(cookieId, response.getFirstHeader("Set-Cookie").getValue());
            }));

  }

  private static Request GET(final URIBuilder uri) throws Exception {
    return Request.Get(uri.build());
  }

  private static String execute(final Request request, final HttpResponseValidator validator)
      throws Exception {
    HttpResponse resp = request.execute().returnResponse();
    validator.validate(resp);
    return EntityUtils.toString(resp.getEntity());
  }

}
