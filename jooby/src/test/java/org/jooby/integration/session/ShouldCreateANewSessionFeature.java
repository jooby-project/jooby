package org.jooby.integration.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.integration.FilterFeature.HttpResponseValidator;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ShouldCreateANewSessionFeature extends ServerFeature {

  {
    get("/shouldCreateANewSession", req -> req.session().id());
  }

  @Test
  public void shouldCreateANewSession() throws Exception {
    String sessionId = execute(GET(uri("shouldCreateANewSession")), r -> {
      assertEquals(200, r.getStatusLine().getStatusCode());
      assertNotNull(r.getFirstHeader("Set-Cookie"));
    });

    assertNotNull(sessionId);

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
