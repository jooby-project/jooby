package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class DefaultRequestObjectsFeature extends ServerFeature {

  {

    get("/req", req -> {
      return req.require(org.jooby.Request.class).path();
    });

    get("/rsp", req -> {
      req.require(org.jooby.Response.class);
      return "/rsp";
    });

    get("/session", req -> {
      req.require(org.jooby.Session.class);
      return "/session";
    });

  }

  @Test
  public void reqParam() throws Exception {
    request()
        .get("/req")
        .expect("/req");
  }

  @Test
  public void rspParam() throws Exception {
    request()
        .get("/rsp")
        .expect("/rsp");
  }

  @Test
  public void sessionParam() throws Exception {
    request()
        .get("/session")
        .expect("/session");
  }

}
