package org.jooby.integration.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.util.EntityUtils;
import org.jooby.integration.FilterFeature.HttpResponseValidator;
import org.jooby.test.ServerFeature;
import org.junit.Before;
import org.junit.Test;

public class ShouldCreateMultipleSessionsFeature extends ServerFeature {

  private static final CookieStore cookieStore = new BasicCookieStore();

  private static final Executor executor;

  static {

    executor = Executor.newInstance().cookieStore(cookieStore);
  }

  {
    get("/shouldCreateMutipleSessions", req -> {
      return req.session().get("count").map(c -> "updated").orElse("created");
    });
  }

  @Test
  public void shouldCreateMutipleSessions() throws Exception {
    assertEquals("created", execute(GET(uri("shouldCreateMutipleSessions")), r0 -> {
      assertEquals(200, r0.getStatusLine().getStatusCode());
      assertNotNull(r0.getFirstHeader("Set-Cookie"));

      resetCookies();

      assertEquals("created", execute(GET(uri("shouldCreateMutipleSessions")), r1 -> {
        assertEquals(200, r1.getStatusLine().getStatusCode());
        assertNotNull(r1.getFirstHeader("Set-Cookie"));
      }));
    }));
  }

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
