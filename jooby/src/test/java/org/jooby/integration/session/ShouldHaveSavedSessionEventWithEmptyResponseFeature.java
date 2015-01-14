package org.jooby.integration.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

public class ShouldHaveSavedSessionEventWithEmptyResponseFeature extends ServerFeature {

  private static final CountDownLatch latch = new CountDownLatch(1);

  {
    use(new Session.MemoryStore() {

      @Override
      public void create(final Session session) {
        super.create(session);
        latch.countDown();
      }
    });

    get("/shouldHaveSavedSessionEvenWithEmptyResponse", (req, rsp) -> {
      req.session().set("k1", "v1");
      rsp.status(200);
    });
  }

  @Test
  public void shouldHaveSavedSessionEvenWithEmptyResponse() throws Exception {
    assertEquals("", execute(GET(uri("shouldHaveSavedSessionEvenWithEmptyResponse")), r0 -> {
      assertEquals(200, r0.getStatusLine().getStatusCode());
      assertNotNull(r0.getFirstHeader("Set-Cookie"));
    }));

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
