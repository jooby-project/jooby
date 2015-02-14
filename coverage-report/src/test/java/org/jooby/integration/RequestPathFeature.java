package org.jooby.integration;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class RequestPathFeature extends ServerFeature {

  {

    get("/hello", (req, rsp) -> rsp.send(req.path()));

    get("/u/p:id", (req, rsp) -> rsp.send(req.path()));

  }

  @Test
  public void requestPath() throws Exception {
    request()
        .get("/hello")
        .expect("/hello");
  }

  @Test
  public void varRequestPath() throws Exception {
    request()
        .get("/u/p1")
        .expect("/u/p1");
  }

}
