package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class RequestPathFeature extends ServerFeature {

  {

    get("/hello", req -> req.contextPath() + req.path());

    get("/u/p:id", req -> req.contextPath() + req.path());

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
