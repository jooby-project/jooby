package org.jooby.integration.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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

public class SessionWithNoTimeOutFeature extends ServerFeature {

  {
    use(new Session.MemoryStore()).timeout(-1);

    get("/session", (req, rsp) -> {
      Session session = req.session();
      assertEquals(-1, session.expiryAt());
      rsp.send(session);
    });

  }

  @Test
  public void sessionWithNoTimeOff() throws Exception {
    execute(GET(uri("session")), r0 -> {
      assertEquals(200, r0.getStatusLine().getStatusCode());
      assertNotNull(r0.getFirstHeader("Set-Cookie"));
      execute(GET(uri("session")), r1 -> {
        assertEquals(200, r1.getStatusLine().getStatusCode());
        assertNull(r1.getFirstHeader("Set-Cookie"));
      });
    });
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
