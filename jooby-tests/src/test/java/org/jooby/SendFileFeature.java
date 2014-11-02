package org.jooby;

import static org.junit.Assert.assertEquals;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.FilterFeature.HttpResponseValidator;
import org.junit.Test;

public class SendFileFeature extends ServerFeature {

  {

    get("/download", (req, rsp) -> {
      rsp.download("/assets/file.js");
    });

  }

  @Test
  public void download() throws Exception {
    assertEquals("function () {}\n", execute(GET(uri("download")), (response) -> {
      assertEquals(200, response.getStatusLine().getStatusCode());
      assertEquals("attachment; filename=file.js",
          response.getFirstHeader("Content-Disposition").getValue());
      assertEquals("chunked", response.getFirstHeader("Transfer-Encoding").getValue());
      assertEquals("application/javascript", response.getFirstHeader("Content-Type").getValue());
    }));
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
