package org.jooby.issues;

import static org.junit.Assert.assertEquals;

import java.util.Optional;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.Err;
import org.jooby.Status;
import org.jooby.integration.FilterFeature.HttpResponseValidator;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue23 extends ServerFeature {

  public static class Mvc {

    @GET
    @Path("/")
    public String handle(final Optional<String> value) {
      return value.orElseThrow(() -> new Err(Status.NOT_FOUND));
    }
  }

  {
    use(Mvc.class);
  }

  @Test
  public void shouldGetStatusWhenErrIsThrownFromMvcRoute() throws Exception {
    execute(GET(uri("/")), (response) -> {
      assertEquals(404, response.getStatusLine().getStatusCode());
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
