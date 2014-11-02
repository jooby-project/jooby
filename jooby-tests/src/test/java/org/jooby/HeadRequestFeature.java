package org.jooby;

import static org.junit.Assert.assertEquals;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.jooby.FilterFeature.HttpResponseValidator;
import org.junit.Test;

public class HeadRequestFeature extends ServerFeature {

  {
    get("/", (req, rsp) -> rsp.send(req.route().verb()));

    // custom head
    head("/head", (req, rsp) -> rsp.send(req.path()));

    // global head
    head("*");
  }

  @Test
  public void defaultHead() throws Exception {
    assertEquals(null, execute(HEAD(uri("/")), (response) -> {
      assertEquals(200, response.getStatusLine().getStatusCode());
    }));
  }

  @Test
  public void realHead() throws Exception {
    assertEquals(null, execute(HEAD(uri("/head")), (response) -> {
      assertEquals(200, response.getStatusLine().getStatusCode());
    }));
  }

  private static Request HEAD(final URIBuilder uri) throws Exception {
    return Request.Head(uri.build());
  }

  private static Object execute(final Request request, final HttpResponseValidator validator)
      throws Exception {
    HttpResponse resp = request.execute().returnResponse();
    validator.validate(resp);
    return resp.getEntity();
  }
}
