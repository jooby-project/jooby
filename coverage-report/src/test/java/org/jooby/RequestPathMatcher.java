package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class RequestPathMatcher extends ServerFeature {

  {
    get("/static/", req -> req.matches("/static"));

    get("/static/f.js", req -> req.matches("/static/**"));

    get("/admin/**", req -> req.matches("/admin/**"));
  }

  @Test
  public void pathMatcher() throws Exception {
    request()
        .get("/static")
        .expect("true");

    request()
        .get("/static/f.js")
        .expect("true");

    request()
        .get("/admin")
        .expect("true");

    request()
        .get("/admin/people")
        .expect("true");
  }
}
