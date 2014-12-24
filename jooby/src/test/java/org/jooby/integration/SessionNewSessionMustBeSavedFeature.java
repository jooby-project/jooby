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

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class SessionNewSessionMustBeSavedFeature extends ServerFeature {

  private static final CountDownLatch saveCalls = new CountDownLatch(1);

  {
    use(ConfigFactory.empty().withValue("application.secret",
        ConfigValueFactory.fromAnyRef("fixed")));

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

    });

    get("/create", (req, rsp) -> {
      rsp.send(req.session().id());
    });

    get("/get", (req, rsp) -> {
      rsp.send(req.session().id());
    });

  }

  @Test
  public void newSessionsMustbeSaved() throws Exception {
    LinkedList<String> setCookie = new LinkedList<>();

    execute(GET(uri("create")), response -> {
      assertEquals(200, response.getStatusLine().getStatusCode());
      assertNotNull(response.getFirstHeader("Set-Cookie").getValue());
      setCookie.add(response.getFirstHeader("Set-Cookie").getValue());
    });

    String sessionId = setCookie.getLast();
    sessionId = sessionId.substring(sessionId.indexOf('=') + 1, sessionId.indexOf('|'));
    String reqCookie = setCookie.getLast().replace("path", "$Path");

    assertEquals(
        sessionId,
        execute(
            GET(uri("get")).addHeader("Cookie", reqCookie),
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
