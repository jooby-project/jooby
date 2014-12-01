package org.jooby.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.integration.FilterFeature.HttpResponseValidator;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ErrDefaultHandlerFeature extends ServerFeature {

  {
    get("/", (req, rsp) -> {
      throw new IllegalArgumentException();
    });

  }

  @Test
  public void err() throws Exception {
    String page = (String) execute(GET(uri("/")), (response) -> {
      assertEquals(400, response.getStatusLine().getStatusCode());
    });
    assertTrue(page.startsWith("<!doctype html><html>\n" +
        "<head>\n" +
        "<meta charset=\"UTF-8\">\n" +
        "<style>\n" +
        "body {font-family: \"open sans\",sans-serif; margin-left: 20px;}\n" +
        "h1 {font-weight: 300; line-height: 44px; margin: 25px 0 0 0;}\n" +
        "h2 {font-size: 16px;font-weight: 300; line-height: 44px; margin: 0;}\n" +
        "footer {font-weight: 300; line-height: 44px; margin-top: 10px;}\n" +
        "hr {background-color: #f7f7f9;}\n" +
        "div.trace {border:1px solid #e1e1e8; background-color: #f7f7f9;}\n" +
        "p {padding-left: 20px;}\n" +
        "p.tab {padding-left: 40px;}\n" +
        "</style>\n" +
        "<title>\n" +
        "400 Bad Request\n" +
        "</title>\n" +
        "<body>\n" +
        "<h1>Bad Request</h1>\n" +
        "<hr><h2>message: Bad Request</h2>\n" +
        "<h2>status: 400</h2>\n" +
        "<h2>referer: </h2>\n" +
        "<h2>stack:</h2>\n"));
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
