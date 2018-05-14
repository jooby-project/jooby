package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue250 extends ServerFeature {

  {
    get("/", req -> {
      req.port();
      return req.header("host").value() + ":" + req.port();
    });
  }

  @Test
  public void defaultPort() throws Exception {
    request().get("/")
        .header("host", "3edfd8c1.ngrok.io")
        .expect("3edfd8c1.ngrok.io:80");
  }

}
