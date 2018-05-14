package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ParamMapFeature extends ServerFeature {

  {
    post("/params/:p1", req -> req.params().toMap());

    post("/sparams/:p1", req -> req.param("p1").toMap());

    post("/bparams/", req -> req.body().toMap().entrySet().iterator().next().getValue().value());
  }

  @Test
  public void params() throws Exception {
    request()
        .post("/params/0")
        .expect("{p1=[0]}");

    request()
        .post("/sparams/0")
        .expect("{p1=[0]}");

    request()
        .post("/bparams")
        .body("0", "text/plain")
        .expect("0");

  }

}
