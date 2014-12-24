package org.jooby.integration;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.Session;
import org.jooby.integration.FilterFeature.HttpResponseValidator;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class SessionRestoreFeature extends ServerFeature {

  private static final CountDownLatch latch = new CountDownLatch(1);

  private static final long createdAt = System.currentTimeMillis() - 500;

  private static final long lastAccessed = System.currentTimeMillis() - 200;

  {
    use(new Session.Store() {
      @Override
      public void save(final Session session, final SaveReason reason) {
      }

      @Override
      public Session get(final Session.Builder builder) {
        latch.countDown();
        return builder
              .accessedAt(lastAccessed)
              .createdAt(createdAt)
              .set("k1", "v1")
              .build();
      }

      @Override
      public void delete(final String id) {
      }

      @Override
      public String generateID(final long seed) {
        return "123";
      }

    });

    get("/restore", (req, rsp) -> {
      Session session = req.session();
      rsp.send(session.createdAt() + "; " + session.accessedAt() + "; " + session.attributes());
    });

  }

  @Test
  public void restoreSession() throws Exception {
    assertEquals(
        createdAt + "; " + lastAccessed + "; {k1=v1}",
        execute(
            GET(uri("restore")).addHeader("Cookie", "jooby.sid=123"),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
            }));

    latch.await();
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
