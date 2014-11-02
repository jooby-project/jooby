package org.jooby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.FilterFeature.HttpResponseValidator;
import org.junit.Test;

public class ErrHandlerFeature extends ServerFeature {

  {
    get("/", (req, rsp) -> {
      throw new IllegalArgumentException();
    });

    err((req, rsp, ex) -> {
      log.error("err", ex);
      assertTrue(ex instanceof IllegalArgumentException);
      assertEquals(Status.BAD_REQUEST, rsp.status().get());
      rsp.send("err...");
    });
  }

  @Test
  public void err() throws Exception {
    assertEquals("err...", execute(GET(uri("/")), (response) -> {
      assertEquals(400, response.getStatusLine().getStatusCode());
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
