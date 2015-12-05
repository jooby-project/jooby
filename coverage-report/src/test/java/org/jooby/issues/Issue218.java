package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue218 extends ServerFeature {

  {
    post("/jsonbug", req -> req.body().value());
  }

  @Test
  public void shouldMatchRoot() throws Exception {
    request()
        .post("/jsonbug")
        .body("\"name\": \"hey && you\"", "application/json")
        .expect(200);
  }
}
