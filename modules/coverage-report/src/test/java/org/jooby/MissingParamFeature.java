package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class MissingParamFeature extends ServerFeature {

  {
    get("/missing", req -> req.param("p1").value());
  }

  @Test
  public void missingParam() throws Exception {
    request()
        .get("/missing")
        .expect(400);
  }
}
