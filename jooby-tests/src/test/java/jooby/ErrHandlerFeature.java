package jooby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import jooby.FilterFeature.HttpResponseValidator;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

public class ErrHandlerFeature extends ServerFeature {

  {
    get("/", (req, res) -> {
      throw new IllegalArgumentException();
    });

    err((req, res, ex) -> {
      log.error("err", ex);
      assertTrue(ex instanceof IllegalArgumentException);
      assertEquals(HttpStatus.BAD_REQUEST, res.status());
      res.send("err...");
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
