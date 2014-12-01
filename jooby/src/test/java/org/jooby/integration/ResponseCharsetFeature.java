package org.jooby.integration;

import static org.junit.Assert.assertEquals;

import java.nio.charset.Charset;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.integration.FilterFeature.HttpResponseValidator;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ResponseCharsetFeature extends ServerFeature {

  {
    get("/charset", (req, rsp) -> {
      Charset charset = req.param("charset").toOptional(Charset.class).orElse(rsp.charset());
      rsp.charset(charset).send("text");
    });
  }

  @Test
  public void defCharset() throws Exception {
    execute(GET(uri("charset")), (rsp) -> {
      assertEquals("text/html; charset=UTF-8", rsp.getFirstHeader("Content-Type").getValue());
    });
  }

  @Test
  public void customCharset() throws Exception {
    execute(GET(uri("charset?charset=UTF-16")), (rsp) -> {
      assertEquals("text/html; charset=UTF-16", rsp.getFirstHeader("Content-Type").getValue());
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
