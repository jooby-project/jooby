package org.jooby.issues;

import static org.junit.Assert.assertEquals;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.integration.FilterFeature.HttpResponseValidator;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue30 extends ServerFeature {

  {

    get("/def-end", (req, rsp) -> {
      rsp.status(200);
    });

    get("/force", (req, rsp) -> {
      rsp.end();
    });

    get("/force", (req, rsp) -> {
      throw new IllegalStateException("Should never get here");
    });
  }

  @Test
  public void defEnd() throws Exception {
    execute(GET(uri("def-end")), rsp -> {
      assertEquals("0", rsp.getFirstHeader("Content-Length").getValue());
    });
  }

  @Test
  public void force() throws Exception {
    execute(GET(uri("force")), rsp -> {
      assertEquals(200, rsp.getStatusLine().getStatusCode());
      assertEquals("0", rsp.getFirstHeader("Content-Length").getValue());
    });
  }

  private static Request GET(final URIBuilder uri) throws Exception {
    return Request.Get(uri.build());
  }

  private static String execute(final Request request, final HttpResponseValidator validator)
      throws Exception {
    HttpResponse resp = request.execute().returnResponse();
    validator.validate(resp);
    return EntityUtils.toString(resp.getEntity());
  }
}
