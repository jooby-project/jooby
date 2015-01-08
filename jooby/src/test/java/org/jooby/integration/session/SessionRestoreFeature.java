package org.jooby.integration.session;

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.util.EntityUtils;
import org.jooby.Session;
import org.jooby.integration.FilterFeature.HttpResponseValidator;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class SessionRestoreFeature extends ServerFeature {

  private static final CountDownLatch latch = new CountDownLatch(1);

  private static final long createdAt = System.currentTimeMillis() - 500;

  private static final long lastAccessed = System.currentTimeMillis() - 200;

  private static final long lastSaved = lastAccessed;

  private static final long expiryAt = lastAccessed + 2000;

  {
    use(new Session.MemoryStore() {
      @Override
      public Session get(final Session.Builder builder) {
        assertEquals("123", builder.sessionId());
        Map<String, Object> attrs = new LinkedHashMap<String, Object>();
        attrs.put("k1", "v1.1");
        Session session = builder
            .accessedAt(lastAccessed)
            .createdAt(createdAt)
            .savedAt(lastSaved)
            .set("k1", "v1")
            .set(attrs)
            .build();
        latch.countDown();
        return session;
      }

    }).timeout(2);

    get("/restore",
        (req, rsp) -> {
          Session session = req.session();
          rsp.send(session.createdAt() + "; " + session.accessedAt() + "; " + session.savedAt()
              + "; " + session.expiryAt()
              + "; " + session.attributes());
        });

  }

  @Test
  public void shouldRestoreSessionFromCookieID() throws Exception {
    assertEquals(createdAt + "; " + lastAccessed + "; " + lastSaved + "; " + expiryAt
        + "; {k1=v1.1}",
        execute(GET(uri("restore")).addHeader("Cookie", "jooby.sid=123"), rsp -> {
          assertEquals(200, rsp.getStatusLine().getStatusCode());
        }));

    latch.await();
  }

  private static Request GET(final URIBuilder uri) throws Exception {
    return Request.Get(uri.build());
  }

  private static String execute(final Request request, final HttpResponseValidator validator)
      throws Exception {
    HttpResponse resp = Executor.newInstance().cookieStore(new BasicCookieStore()).execute(request)
        .returnResponse();
    validator.validate(resp);
    return EntityUtils.toString(resp.getEntity());
  }

}
