package org.jooby;

import static org.junit.Assert.assertEquals;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.FilterFeature.HttpResponseValidator;
import org.junit.Test;

public class OptionsRequestFeature extends ServerFeature {

  {
    get("/", (req, res) -> res.send(req.route().verb()));

    post("/", (req, res) -> res.send(req.route().verb()));

    get("/sub", (req, res) -> res.send(req.route().verb()));

    post("/sub", (req, res) -> res.send(req.route().verb()));

    delete("/sub", (req, res) -> res.send(req.route().verb()));

    options("*");
  }

  @Test
  public void defaultOptions() throws Exception {
    assertEquals("", execute(OPTIONS(uri("/")), (response) -> {
      assertEquals(200, response.getStatusLine().getStatusCode());
      assertEquals("GET, POST", response.getFirstHeader("Allow").getValue());
      assertEquals("0", response.getFirstHeader("Content-Length").getValue());
    }));
  }

  @Test
  public void subPathOptions() throws Exception {
    assertEquals("", execute(OPTIONS(uri("/sub")), (response) -> {
      assertEquals(200, response.getStatusLine().getStatusCode());
      assertEquals("GET, POST, DELETE", response.getFirstHeader("Allow").getValue());
      assertEquals("0", response.getFirstHeader("Content-Length").getValue());
    }));
  }


  private static Request OPTIONS(final URIBuilder uri) throws Exception {
    return Request.Options(uri.build());
  }

  private static Object execute(final Request request, final HttpResponseValidator validator)
      throws Exception {
    HttpResponse resp = request.execute().returnResponse();
    validator.validate(resp);
    return EntityUtils.toString(resp.getEntity());
  }
}
