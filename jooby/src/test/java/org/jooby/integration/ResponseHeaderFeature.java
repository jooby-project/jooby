package org.jooby.integration;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.integration.FilterFeature.HttpResponseValidator;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ResponseHeaderFeature extends ServerFeature {

  {
    get("/headers", (req, rsp) -> {
      rsp.header("byte", (byte) 7);
      rsp.header("char", 'c');
      rsp.header("double", 7.0d);
      rsp.header("float", 2.1f);
      rsp.header("int", 4);
      rsp.header("long", 2L);
      rsp.header("date", new Date(1416700709415L));
      rsp.header("short", (short) 43);
      rsp.header("str", "str");
      rsp.status(200);
    });
  }

  @Test
  public void headers() throws Exception {
    execute(GET(uri("headers")), (rsp) -> {
      assertEquals(200, rsp.getStatusLine().getStatusCode());

      assertEquals("7", rsp.getFirstHeader("byte").getValue());
      assertEquals("c", rsp.getFirstHeader("char").getValue());
      assertEquals("7.0", rsp.getFirstHeader("double").getValue());
      assertEquals("2.1", rsp.getFirstHeader("float").getValue());
      assertEquals("4", rsp.getFirstHeader("int").getValue());
      assertEquals("2", rsp.getFirstHeader("long").getValue());
      assertEquals("Sat, 22 Nov 2014 23:58:29 GMT", rsp.getFirstHeader("date").getValue());
      assertEquals("43", rsp.getFirstHeader("short").getValue());
      assertEquals("str", rsp.getFirstHeader("str").getValue());
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
