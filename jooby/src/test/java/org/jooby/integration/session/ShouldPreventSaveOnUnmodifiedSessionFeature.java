package org.jooby.integration.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.Session;
import org.jooby.integration.FilterFeature.HttpResponseValidator;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ShouldPreventSaveOnUnmodifiedSessionFeature extends ServerFeature {

  private static final CountDownLatch latch = new CountDownLatch(2);

  {

    use(new Session.MemoryStore() {

      @Override
      public void create(final Session session) {
        super.create(session);
        latch.countDown();
      }

      @Override
      public void save(final Session session) {
        super.save(session);
        latch.countDown();
      }
    });

    get("/shouldPreventSaveOnUnmodifiedSession", req -> {
      Session session = req.session();
      session.set("k1", "v1");
      return session.get("k1").get();
    });

  }

  @Test
  public void shouldPreventSaveOnUnmodifiedSession() throws Exception {

    execute(GET(uri("shouldPreventSaveOnUnmodifiedSession")), r -> {
      assertEquals(200, r.getStatusLine().getStatusCode());
      assertNotNull(r.getFirstHeader("Set-Cookie"));
    });

    execute(GET(uri("shouldPreventSaveOnUnmodifiedSession")), r -> {
      assertEquals(200, r.getStatusLine().getStatusCode());
    });

    execute(GET(uri("shouldPreventSaveOnUnmodifiedSession")), r -> {
      assertEquals(200, r.getStatusLine().getStatusCode());
    });

    latch.await(1000, TimeUnit.MILLISECONDS);
    assertEquals(1, latch.getCount());
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
