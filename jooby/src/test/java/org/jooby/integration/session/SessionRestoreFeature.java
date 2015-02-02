package org.jooby.integration.session;

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

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

public class SessionRestoreFeature extends ServerFeature {

  private static final CountDownLatch latch = new CountDownLatch(1);

  private static final AtomicLong createdAt = new AtomicLong();

  private static final AtomicLong lastAccessed = new AtomicLong();

  private static final AtomicLong lastSaved = new AtomicLong();

  private static final AtomicLong expiryAt = new AtomicLong();

  {
    createdAt.set(System.currentTimeMillis() - 1000);
    lastAccessed.set(System.currentTimeMillis() - 200);
    lastSaved.set(lastAccessed.get());
    expiryAt.set(lastAccessed.get() + 2000);
    use(new Session.MemoryStore() {
      @Override
      public Session get(final Session.Builder builder) {
        assertEquals("678", builder.sessionId());
        Map<String, Object> attrs = new LinkedHashMap<String, Object>();
        attrs.put("k1", "v1.1");
        Session session = builder
            .accessedAt(lastAccessed.get())
            .createdAt(createdAt.get())
            .savedAt(lastSaved.get())
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
        execute(GET(uri("restore")).addHeader("Cookie", "jooby.sid=678"), rsp -> {
          assertEquals(200, rsp.getStatusLine().getStatusCode());
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
