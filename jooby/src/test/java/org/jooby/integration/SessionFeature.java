package org.jooby.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.Session;
import org.jooby.integration.FilterFeature.HttpResponseValidator;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class SessionFeature extends ServerFeature {

  @Path("r")
  public static class Resource {

    @org.jooby.mvc.GET
    @Path("session")
    public Object session(final Session session) {
      return session.get("saves").orElse(0);
    }

    @org.jooby.mvc.GET
    @Path("ifSession")
    public Object ifSession(final Optional<Session> session) {
      return session.get().get("saves").orElse(0);
    }

  }

  private static final CountDownLatch delete = new CountDownLatch(1);

  {

    use(ConfigFactory.empty().withValue("application.secret",
        ConfigValueFactory.fromAnyRef("fixed")));

    use(new Session.Store() {
      @Override
      public void save(final Session session, final SaveReason reason) {
        assertNotNull(session);
      }

      @Override
      public Session get(final Session.Builder builder) {
        return null;
      }

      @Override
      public void delete(final String id) {
        delete.countDown();
      }

    }).timeout(3);

    get("/no-session", (req, rsp) -> {
      rsp.send(req.ifSession());
    });

    get("/session", (req, rsp) -> {
      Integer saves = req.session().<Integer> get("saves").orElse(0);
      req.session().set("saves", saves + 1);
      rsp.send(saves);
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

    use(Resource.class);
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
    LinkedList<String> cookieId = new LinkedList<>();
    assertEquals(
        "0",
        execute(
            GET(uri("session")),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
              assertNotNull(response.getFirstHeader("Set-Cookie").getValue());
              cookieId.add(response.getFirstHeader("Set-Cookie").getValue());
            }));

    assertEquals(
        "1",
        execute(
            GET(uri("r", "session")).addHeader("Cookie",
                cookieId.getLast().replace("path", "$Path")),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
              assertNull(response.getFirstHeader("Set-Cookie"));
            }));

    assertEquals(
        "1",
        execute(
            GET(uri("session")).addHeader("Cookie", cookieId.getLast().replace("path", "$Path")),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
              assertNull(response.getFirstHeader("Set-Cookie"));
            }));

    assertEquals(
        "2",
        execute(
            GET(uri("r", "ifSession")).addHeader("Cookie",
                cookieId.getLast().replace("path", "$Path")),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
              assertNull(response.getFirstHeader("Set-Cookie"));
            }));

  }

  @Test
  public void newSessions() throws Exception {
    String[] cookieId = {"" };
    assertEquals(
        "0",
        execute(
            GET(uri("session")),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
              assertNotNull(response.getFirstHeader("Set-Cookie").getValue());
              cookieId[0] = response.getFirstHeader("Set-Cookie").getValue();
            }));

    assertEquals(
        "0",
        execute(
            GET(uri("session")),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
              assertNotNull(response.getFirstHeader("Set-Cookie").getValue());
            }));

    assertEquals(
        "0",
        execute(
            GET(uri("session")),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
              assertNotNull(response.getFirstHeader("Set-Cookie").getValue());
            }));
  }

  @Test
  public void tamperedSession() throws Exception {
    LinkedList<String> cookieIds = new LinkedList<String>();
    assertEquals(
        "0",
        execute(
            GET(uri("session")),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
              assertNotNull(response.getFirstHeader("Set-Cookie").getValue());
              cookieIds.add(response.getFirstHeader("Set-Cookie").getValue());
            }));

    String tamperedId1 = cookieIds.getLast();
    tamperedId1 = "jooby.sid=6590" + tamperedId1.substring(tamperedId1.indexOf("|"));

    assertEquals(
        "0",
        execute(
            GET(uri("session")).addHeader("Cookie", tamperedId1),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
              assertNotNull(response.getFirstHeader("Set-Cookie").getValue());
              assertNotEquals(cookieIds.getLast(), response.getFirstHeader("Set-Cookie").getValue());
              cookieIds.add(response.getFirstHeader("Set-Cookie").getValue());
            }));

    String tamperedId2 = cookieIds.getFirst();
    tamperedId2 = tamperedId2.substring(0, tamperedId2.indexOf("|")) + "|xxu1u1u1o1odd";

    assertEquals(
        "0",
        execute(
            GET(uri("session")).addHeader("Cookie", tamperedId2),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
              assertNotNull(response.getFirstHeader("Set-Cookie").getValue());
              assertNotEquals(cookieIds.getLast(), response.getFirstHeader("Set-Cookie").getValue());
              cookieIds.add(response.getFirstHeader("Set-Cookie").getValue());
            }));

    assertEquals(
        "0",
        execute(
            GET(uri("session")).addHeader("Cookie", "jooby.id=1234; $Path=/; secure; HttpOnly"),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
              assertNotNull(response.getFirstHeader("Set-Cookie").getValue());
              assertNotEquals(cookieIds.getLast(), response.getFirstHeader("Set-Cookie").getValue());
            }));
  }

  @Test
  public void time() throws Exception {
    LinkedList<String> cookieIds = new LinkedList<String>();

    long createdAt = Long.parseLong(execute(
        GET(uri("session/0")),
        (response) -> {
          assertEquals(200, response.getStatusLine().getStatusCode());
          assertNotNull(response.getFirstHeader("Set-Cookie").getValue());
          cookieIds.add(response.getFirstHeader("Set-Cookie").getValue());
        }));
    assertTrue(createdAt > 0);

    long access1 = Long.parseLong(execute(
        GET(uri("session/1")).addHeader("Cookie", cookieIds.getLast().replace("path", "$Path")),
        (response) -> {
          assertEquals(200, response.getStatusLine().getStatusCode());
        }));
    assertTrue(access1 >= createdAt);
  }

  @Test
  public void timeout() throws Exception {
    LinkedList<String> cookieIds = new LinkedList<String>();
    assertEquals(
        "0",
        execute(
            GET(uri("session")),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
              assertNotNull(response.getFirstHeader("Set-Cookie").getValue());
              cookieIds.add(response.getFirstHeader("Set-Cookie").getValue());
            }));

    assertEquals(
        "1",
        execute(
            GET(uri("session")).addHeader("Cookie", cookieIds.getLast().replace("path", "$Path")),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
            }));

    Thread.sleep(1000L);
    assertEquals(
        "2",
        execute(
            GET(uri("session")).addHeader("Cookie", cookieIds.getLast().replace("path", "$Path")),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
            }));

    Thread.sleep(1500L);
    assertEquals(
        "3",
        execute(
            GET(uri("session")).addHeader("Cookie", cookieIds.getLast().replace("path", "$Path")),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
            }));

    Thread.sleep(3000L);
    assertEquals(
        "0",
        execute(
            GET(uri("session")).addHeader("Cookie", cookieIds.getLast().replace("path", "$Path")),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
              assertNotEquals(cookieIds.getLast(), response.getFirstHeader("Set-Cookie").getValue());
            }));

    delete.await();
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
