package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue946 extends ServerFeature {

  {
    path("/946/api/some", () -> {
      path("/:id", () -> {
        get(req -> req.param("id").value());

        get("/enabled", req -> req.param("id").value());
      });
    });
  }

  @Test
  public void nestedPathExpression() throws Exception {
    request().get("/946/api/some/1")
        .expect("1");
    request().get("/946/api/some/2/enabled")
        .expect("2");
  }

}
