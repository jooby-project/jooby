package org.jooby.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.Session;
import org.jooby.integration.FilterFeature.HttpResponseValidator;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class SessionSaveIntervalFeature extends ServerFeature {

  private static final CountDownLatch saveCalls = new CountDownLatch(2);

  {
    use(new Session.Store() {
      @Override
      public void save(final Session session, final SaveReason reason) {
        saveCalls.countDown();
      }

      @Override
      public Session get(final Session.Builder builder) {
        return null;
      }

      @Override
      public void delete(final String id) {
      }

    }).saveInterval(2);

    get("/create", (req, rsp) -> {
      rsp.send(req.session().id());
    });

    get("/touch", (req, rsp) -> {
      rsp.send(req.session().id());
    });

  }

  @Test
  public void sessionMustBeSavedOnSaveInterval() throws Exception {
    LinkedList<String> setCookie = new LinkedList<>();

    execute(GET(uri("create")), response -> {
      assertEquals(200, response.getStatusLine().getStatusCode());
      assertNotNull(response.getFirstHeader("Set-Cookie").getValue());
      setCookie.add(response.getFirstHeader("Set-Cookie").getValue());
    });

    String sessionId = setCookie.getLast();
    sessionId = sessionId.substring(sessionId.indexOf('=') + 1, sessionId.indexOf(';')).trim();
    String reqCookie = setCookie.getLast().replace("path", "$Path");

    assertEquals(
        sessionId,
        execute(
            GET(uri("touch")).addHeader("Cookie", reqCookie),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
            }));

    assertEquals(
        sessionId,
        execute(
            GET(uri("touch")).addHeader("Cookie", reqCookie),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
            }));

    Thread.sleep(2000L);

    assertEquals(
        sessionId,
        execute(
            GET(uri("touch")).addHeader("Cookie", reqCookie),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
            }));

    saveCalls.await();
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
