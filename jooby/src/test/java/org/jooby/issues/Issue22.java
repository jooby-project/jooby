package org.jooby.issues;

import static org.junit.Assert.assertEquals;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.integration.FilterFeature.HttpResponseValidator;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue22 extends ServerFeature {

  {

    use(ConfigFactory.empty()
        .withValue("application.path", ConfigValueFactory.fromAnyRef("/demo")));

    get("/", () -> "/");

    get("/path", () -> "path");
  }

  @Test
  public void appShouldBeMountedOnApplicationPath() throws Exception {
    assertEquals("/", execute(GET(uri("/demo")), r -> {
      assertEquals(200, r.getStatusLine().getStatusCode());
    }));

    assertEquals("path", execute(GET(uri("/demo/path")), r -> {
      assertEquals(200, r.getStatusLine().getStatusCode());
    }));
  }

  @Test
  public void wrongPathShouldResolveAs404() throws Exception {
    execute(GET(uri("/")), r -> {
      assertEquals(404, r.getStatusLine().getStatusCode());
    });

    execute(GET(uri("/path")), r -> {
      assertEquals(404, r.getStatusLine().getStatusCode());
    });
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
