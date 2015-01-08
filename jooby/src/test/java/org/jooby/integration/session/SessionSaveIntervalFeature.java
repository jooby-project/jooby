package org.jooby.integration.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.CountDownLatch;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Executor;
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
    use(new Session.MemoryStore() {
      @Override
      public void save(final Session session) {
        saveCalls.countDown();
      }

    }).saveInterval(1);

    get("/session", (req, rsp) -> {
      rsp.send(req.session().id());
    });

  }

  @Test
  public void sessionMustBeSavedOnSaveInterval() throws Exception {
    execute(GET(uri("session")), rsp -> {
      assertEquals(200, rsp.getStatusLine().getStatusCode());
      assertNotNull(rsp.getFirstHeader("Set-Cookie").getValue());
    });

    execute(GET(uri("session")), rsp -> {
      assertEquals(200, rsp.getStatusLine().getStatusCode());
      assertNull(rsp.getFirstHeader("Set-Cookie"));
    });

    execute(GET(uri("session")), rsp -> {
      assertEquals(200, rsp.getStatusLine().getStatusCode());
      assertNull(rsp.getFirstHeader("Set-Cookie"));
    });

    Thread.sleep(1200L);

    execute(GET(uri("session")), rsp -> {
      assertEquals(200, rsp.getStatusLine().getStatusCode());
      assertNull(rsp.getFirstHeader("Set-Cookie"));
    });

    saveCalls.await();
  }

  private static Request GET(final URIBuilder uri) throws Exception {
    return Request.Get(uri.build());
  }

  private static String execute(final Request request, final HttpResponseValidator validator)
      throws Exception {
    HttpResponse resp = Executor.newInstance().execute(request).returnResponse();
    validator.validate(resp);
    return EntityUtils.toString(resp.getEntity());
  }

}
