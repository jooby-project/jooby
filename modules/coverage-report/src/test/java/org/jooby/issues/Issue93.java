package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue93 extends ServerFeature {

  {
    get("/search/**", req -> req.path());
  }

  @Test
  public void shouldMatchRoot() throws Exception {
    request()
        .get("/search")
        .expect("/search");
  }
}
