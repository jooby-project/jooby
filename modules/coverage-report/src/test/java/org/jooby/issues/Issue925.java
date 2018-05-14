package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue925 extends ServerFeature {

  {
    path("/925/api/some", () -> {
      get(req -> req.route().produces());
      post(req -> req.route().consumes());
    }).produces("json")
      .consumes("json");
  }

  @Test
  public void pathOperatorWithRouteProperties() throws Exception {
    request().get("/925/api/some")
        .expect("[application/json]");
    request().post("/925/api/some")
        .expect("[application/json]");
  }

}
