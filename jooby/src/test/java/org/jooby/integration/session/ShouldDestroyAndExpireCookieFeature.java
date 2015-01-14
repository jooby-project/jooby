package org.jooby.integration.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;

import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.util.EntityUtils;
import org.jooby.Session;
import org.jooby.integration.FilterFeature.HttpResponseValidator;
import org.jooby.test.ServerFeature;
import org.junit.Before;
import org.junit.Test;

public class ShouldDestroyAndExpireCookieFeature extends ServerFeature {

  private static final CountDownLatch latch = new CountDownLatch(1);

  {
    use(new Session.MemoryStore() {
      @Override
      public void delete(final String id) {
        super.delete(id);
        latch.countDown();
      }

    });

    get("/session", (req, rsp) -> {
      rsp.send(req.session().id());
    });

    get("/invalidate", (req, rsp) -> {
      Session session = req.session();
      assertTrue(req.ifSession().isPresent());
      session.destroy();
      assertTrue(!req.ifSession().isPresent());
      rsp.send("done");
    });

  }

  @Test
  public void shouldDestroyAndExpireCookie() throws Exception {
    execute(GET(uri("session")), rsp -> {
      assertEquals(200, rsp.getStatusLine().getStatusCode());
      assertNotNull(rsp.getFirstHeader("Set-Cookie").getValue());
    });

    execute(GET(uri("invalidate")), rsp -> {
      assertEquals(200, rsp.getStatusLine().getStatusCode());
      assertNotNull(rsp.getFirstHeader("Set-Cookie"));
      String SetCookie = rsp.getFirstHeader("Set-Cookie").getValue();
      assertNotNull(SetCookie);
      assertTrue(SetCookie.endsWith("01-Jan-1970 00:00:00 GMT"));
    });

    latch.await();
  }

  private static final CookieStore cookieStore = new BasicCookieStore();

  private static final Executor executor = Executor.newInstance().cookieStore(cookieStore);

  @Before
  public void resetCookies() {
    cookieStore.clear();
  }

  private static Request GET(final URIBuilder uri) throws Exception {
    return Request.Get(uri.build());
  }

  private static String execute(final Request request, final HttpResponseValidator validator)
      throws Exception {
    HttpResponse resp = executor.execute(request).returnResponse();
    validator.validate(resp);
    return EntityUtils.toString(resp.getEntity());
  }

}
