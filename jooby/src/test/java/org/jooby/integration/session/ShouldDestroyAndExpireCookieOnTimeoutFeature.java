package org.jooby.integration.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

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

public class ShouldDestroyAndExpireCookieOnTimeoutFeature extends ServerFeature {

  private static final CountDownLatch latch = new CountDownLatch(1);

  {
    use(new Session.MemoryStore() {
      @Override
      public void delete(final String id) {
        super.delete(id);
        latch.countDown();
      }

    }).timeout(1);

    get("/session", (req, rsp) -> {
      rsp.send(req.session().id());
    });
  }

  @Test
  public void shouldDestroyAndExpireCookieOnTimeout() throws Exception {
    String sessionId1 = execute(GET(uri("session")), rsp -> {
      assertEquals(200, rsp.getStatusLine().getStatusCode());
      assertNotNull(rsp.getFirstHeader("Set-Cookie").getValue());
    });

    Thread.sleep(1200L);
    String sessionId2 = execute(GET(uri("session")), rsp -> {
      assertEquals(200, rsp.getStatusLine().getStatusCode());
      assertNotNull(rsp.getFirstHeader("Set-Cookie").getValue());
    });

    latch.await();

    assertNotEquals(sessionId1, sessionId2);
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
