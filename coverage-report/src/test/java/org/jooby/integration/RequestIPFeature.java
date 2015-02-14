package org.jooby.integration;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class RequestIPFeature extends ServerFeature {

  {

    get("/ip", (req) -> req.ip());

    get("/hostname", (req) -> req.hostname());

  }

  @Test
  public void ip() throws Exception {
    request()
        .get("/ip")
        .expect("127.0.0.1");
  }

  @Test
  public void hostname() throws Exception {
    request()
        .get("/hostname")
        .expect("localhost");
  }

}
