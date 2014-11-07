package org.jooby.test;

import static org.junit.Assert.assertEquals;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.Cookie;
import org.jooby.test.FilterFeature.HttpResponseValidator;
import org.junit.Test;

public class CookiesFeature extends ServerFeature {

  {

    get("/set", (req, rsp) -> {
      rsp.cookie(new Cookie.Definition("X", "x").path("/set")).send("done");
    });

  }

  @Test
  public void responseCookie() throws Exception {
    assertEquals("done", execute(GET(uri("set")), (response) -> {
      assertEquals(200, response.getStatusLine().getStatusCode());
      assertEquals("X=x;Path=/set", response.getFirstHeader("Set-Cookie").getValue());
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
