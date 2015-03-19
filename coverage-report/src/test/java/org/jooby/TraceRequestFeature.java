package org.jooby;

import static org.junit.Assert.assertTrue;

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
      .header("Content-Length", len -> {
        assertTrue(Integer.parseInt(len) >= 163);
      })
      .startsWith("TRACE");
  }

}
