package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class OptionsRequestFeature extends ServerFeature {

  {
    get("/", (req, rsp) -> rsp.send(req.route().method()));

    post("/", (req, rsp) -> rsp.send(req.route().method()));

    get("/sub", (req, rsp) -> rsp.send(req.route().method()));

    post("/sub", (req, rsp) -> rsp.send(req.route().method()));

    delete("/sub", (req, rsp) -> rsp.send(req.route().method()));

    options();
  }

  @Test
  public void defaultOptions() throws Exception {
    request()
        .options("/")
        .expect(200)
        .header("Allow", "GET, POST")
        .empty();
  }

  @Test
  public void subPathOptions() throws Exception {
    request()
        .options("/sub")
        .expect(200)
        .header("Allow", "GET, POST, DELETE")
        .empty();
  }

}
