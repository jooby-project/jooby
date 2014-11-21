package org.jooby.integration;

import static org.junit.Assert.assertEquals;

import org.apache.http.client.fluent.Request;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class RequestCharsetFeature extends ServerFeature {

  {

    get("/", (req, resp) -> {
      resp.send(req.charset());
    });

    post("/", (req, resp) -> {
      resp.send(req.charset());
    });

  }

  @Test
  public void defaultCharset() throws Exception {
    assertEquals("UTF-8", Request.Get(uri().build()).execute().returnContent().asString());
  }

}
