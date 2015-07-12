package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class MutantIsSetFeature extends ServerFeature {

  {
    get("/isset/param", req -> req.param("noset").isSet());

    get("/isset/params", req -> req.params().isSet());

    post("/isset/body", req -> req.body().isSet());
  }

  @Test
  public void param() throws Exception {
    request()
        .get("/isset/param")
        .expect("false");
  }

  @Test
  public void params() throws Exception {
    request()
        .get("/isset/params")
        .expect("false");
  }

  @Test
  public void body() throws Exception {
    request()
        .post("/isset/body")
        .expect("false");
  }

}
