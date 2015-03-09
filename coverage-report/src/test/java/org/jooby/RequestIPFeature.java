package org.jooby;

import static org.junit.Assert.fail;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class RequestIPFeature extends ServerFeature {

  {

    get("/ip", (req) -> req.ip());

    get("/hostname", (req) -> req.hostname());

    get("/protocol", (req) -> req.protocol());

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
        .expect(rsp -> {
          if ("localhost".equals(rsp) || "127.0.0.1".equals(rsp)) {
            return;
          }
          fail(rsp);
        });
  }

  @Test
  public void protocol() throws Exception {
    request()
        .get("/protocol")
        .expect("HTTP/1.1");
  }

}
