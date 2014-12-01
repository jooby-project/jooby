package org.jooby.integration;

import static org.junit.Assert.assertEquals;

import java.util.NoSuchElementException;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.jooby.Err;
import org.jooby.Status;
import org.jooby.integration.FilterFeature.HttpResponseValidator;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ErrDefaultStatusCodeFeature extends ServerFeature {

  {
    get("/IllegalArgumentException", (req, rsp) -> {
      throw new IllegalArgumentException();
    });

    get("/NoSuchElementException", (req, rsp) -> {
      throw new NoSuchElementException();
    });

    get("/Err", (req, rsp) -> {
      throw new Err(Status.BAD_REQUEST);
    });

    get("/NullPointerException", (req, rsp) -> {
      throw new NullPointerException();
    });

    get("/NotAcceptable", (req, rsp) -> {
      rsp.send(new Object());
    }).produces("json");

    post("/UnsupportedMediaType", (req, rsp) -> {
      rsp.send(req.body(String.class));
    }).consumes("json");

  }

  @Test
  public void errIllegalArgumentException() throws Exception {
    execute(GET(uri("/IllegalArgumentException")), (response) -> {
      assertEquals(400, response.getStatusLine().getStatusCode());
    });
  }

  @Test
  public void errNoSuchElementException() throws Exception {
    execute(GET(uri("/NoSuchElementException")), (response) -> {
      assertEquals(400, response.getStatusLine().getStatusCode());
    });
  }

  @Test
  public void errNullPointerException() throws Exception {
    execute(GET(uri("/NullPointerException")), (response) -> {
      assertEquals(500, response.getStatusLine().getStatusCode());
    });
  }

  @Test
  public void err() throws Exception {
    execute(GET(uri("/Err")), (response) -> {
      assertEquals(400, response.getStatusLine().getStatusCode());
    });
  }

  @Test
  public void NotAcceptable() throws Exception {
    execute(GET(uri("/NotAcceptable")), (response) -> {
      assertEquals(406, response.getStatusLine().getStatusCode());
    });
  }

  @Test
  public void unsupportedMediaType() throws Exception {
    execute(Request
        .Post(uri("UnsupportedMediaType").build())
        .bodyString("<xml><form></form>", ContentType.APPLICATION_XML), (rsp) -> {
      assertEquals(415, rsp.getStatusLine().getStatusCode());
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
