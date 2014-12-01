package org.jooby.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.Session;
import org.jooby.integration.FilterFeature.HttpResponseValidator;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class GetExternalSessionFeature extends ServerFeature {

  static final String sessionId = "1234|YCoA3Xy3SpWxF95bTC+lVLg/GtTCO8YkKFkTeQ15v3E";
  static final String cookieId = "jooby.sid=" + sessionId + ";Path=/;Secure;HttpOnly";

  static final long time = System.currentTimeMillis();

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
        assertEquals(sessionId, builder.sessionId());
        return builder
            .set("v", "persisted")
            .set(ImmutableMap.<String, Object> builder().put("x", "y").build())
            .accessedAt(time)
            .createdAt(time)
            .build();
      }

      @Override
      public void delete(final String id) {
      }

      @Override
      public String generateID(final long seed) {
        return "1234";
      }
    });

    get("/session", (req, rsp) -> {
      rsp.send(req.session().attributes());
    });

    get("/session/0", (req, rsp) -> {
      rsp.send(req.session().createdAt());
    });

    get("/session/1", (req, rsp) -> {
      rsp.send(req.session().accessedAt());
    });

  }

  @Test
  public void session() throws Exception {
    assertEquals(
        "{x=y, v=persisted}",
        execute(
            GET(uri("session")).addHeader("Cookie", cookieId),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
            }));

    assertEquals(
        "" + time,
        execute(
            GET(uri("session/0")).addHeader("Cookie", cookieId),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
            }));

    assertTrue(time < Long.parseLong(
        execute(
            GET(uri("session/1")).addHeader("Cookie", cookieId),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
            })));

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
