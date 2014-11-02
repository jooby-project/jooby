package org.jooby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.FilterFeature.HttpResponseValidator;
import org.junit.Test;

public class TraceRequestFeature extends ServerFeature {

  {
    get("/", (req, rsp) -> rsp.send(req.route().verb()));

    post("/", (req, rsp) -> rsp.send(req.route().verb()));

    get("/sub", (req, rsp) -> rsp.send(req.route().verb()));

    trace("*");

  }

  @Test
  public void trace() throws Exception {
    String rsp = execute(TRACE(uri("/")), (response) -> {
      assertEquals(200, response.getStatusLine().getStatusCode());
      assertTrue(Integer.parseInt(response.getFirstHeader("Content-Length").getValue()) >= 163);
    });
    assertTrue(rsp.startsWith("TRACE"));
  }

  private static Request TRACE(final URIBuilder uri) throws Exception {
    return Request.Trace(uri.build());
  }

  private static String execute(final Request request, final HttpResponseValidator validator)
      throws Exception {
    HttpResponse resp = request.execute().returnResponse();
    validator.validate(resp);
    return EntityUtils.toString(resp.getEntity());
  }
}
