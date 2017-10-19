package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue880 extends ServerFeature {

  {
    caseSensitiveRouting(false);

    get("/880/path", req -> req.path());
    get("/880/path/:id", req -> req.path());
  }

  @Test
  public void shouldIgnoreCase() throws Exception {
    request().get("/880/path")
        .expect("/880/path");
    request().get("/880/Path")
        .expect("/880/Path");

    request().get("/880/PatH")
        .expect("/880/PatH");

    request().get("/880/path/AB")
        .expect("/880/path/AB");

    request().get("/880/path/ab")
        .expect("/880/path/ab");

    request().get("/880/paTh/AB")
        .expect("/880/paTh/AB");
  }

}
