package org.jooby.integration;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class RequestCharsetFeature extends ServerFeature {

  {

    get("/", (req, resp) ->
        resp.send(req.charset()));

  }

  @Test
  public void defaultCharset() throws Exception {
    request()
        .get("/")
        .expect("UTF-8");
  }

}
