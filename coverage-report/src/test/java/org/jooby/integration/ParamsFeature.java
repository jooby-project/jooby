package org.jooby.integration;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ParamsFeature extends ServerFeature {

  {
    post("/params", req -> req.params());
  }

  @Test
  public void params() throws Exception {
    request()
        .post("/params?p1=1")
        .form()
        .add("p1", "2")
        .add("p2", "2")
        .expect("{p1=[1, 2], p2=2}");

  }

}
