package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class TraceRequestFeature extends ServerFeature {

  {
    get("/", (req, rsp) -> rsp.send(req.route().method()));

    post("/", (req, rsp) -> rsp.send(req.route().method()));

    get("/sub", (req, rsp) -> rsp.send(req.route().method()));

    trace();

  }

  @Test
  public void globTrace() throws Exception {
    request()
      .trace("/")
      .expect(200)
      .startsWith("TRACE");
  }

}
