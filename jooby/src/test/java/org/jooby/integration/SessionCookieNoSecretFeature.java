package org.jooby.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.Session;
import org.jooby.integration.FilterFeature.HttpResponseValidator;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

public class SessionCookieNoSecretFeature extends ServerFeature {

  {
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
      }

      @Override
      public String generateID(final long seed) {
        return "1234";
      }

    }).cookie()
        .name("custom.sid")
        .path("/session")
        .maxAge(60);

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
    String sessionId = "1234";
    assertEquals(
        sessionId,
        execute(
            GET(uri("session")),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
              List<String> setCookie = Lists.newArrayList(Splitter.onPattern(";\\s*").splitToList(
                  response.getFirstHeader("Set-Cookie").getValue()));
              assertTrue(setCookie.remove("custom.sid=" + sessionId));
              assertTrue(setCookie.remove("path=/session"));
              assertTrue(setCookie.remove("secure"));
              assertTrue(setCookie.remove("HttpOnly"));
              assertTrue(setCookie.remove("Max-Age=60"));
              assertEquals(1, setCookie.size());
              assertTrue(setCookie.remove(0).startsWith(
                  "Expires=" + formatter.format(utc).replace("GMT", "")));
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
