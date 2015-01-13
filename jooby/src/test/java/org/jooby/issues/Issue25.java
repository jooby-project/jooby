package org.jooby.issues;

import static org.junit.Assert.assertEquals;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.integration.FilterFeature.HttpResponseValidator;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.Config;

public class Issue25 extends ServerFeature {

  {
    get("/contextPath", req -> {
      assertEquals("/", req.get("contextPath").get());
      assertEquals("/", req.require(Config.class).getString("application.path"));
      return req.path();
    });

    get("/req-path", req -> {
      assertEquals("/req-path", req.get("path").get());
      assertEquals("/req-path", req.path());
      return req.path();
    });
  }

  @Test
  public void shouldSetApplicationPath() throws Exception {
    assertEquals("/contextPath", execute(GET(uri("/contextPath")), r -> {
      assertEquals(200, r.getStatusLine().getStatusCode());
    }));
  }

  @Test
  public void shouldSetRequestPath() throws Exception {
    assertEquals("/req-path", execute(GET(uri("/req-path")), r -> {
      assertEquals(200, r.getStatusLine().getStatusCode());
    }));
  }

  private static Request GET(final URIBuilder uri) throws Exception {
    return Request.Get(uri.build());
  }

  private static Object execute(final Request request, final HttpResponseValidator validator)
      throws Exception {
    HttpResponse resp = request.execute().returnResponse();
    validator.validate(resp);
    return EntityUtils.toString(resp.getEntity());
  }
}
