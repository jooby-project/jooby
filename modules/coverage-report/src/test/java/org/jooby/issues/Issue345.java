package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue345 extends ServerFeature {

  {
    get("/a", req -> req.route().attributes());

    with(() -> {

      get("/b", req -> req.route().attributes());

      get("/c", req -> req.route().attributes());

    }).attr("foo", "bar");

    get("/d", req -> req.route().attributes());
  }

  @Test
  public void withOperator() throws Exception {
    request().get("/a")
        .expect("{}");
    request().get("/b")
        .expect("{foo=bar}");
    request().get("/c")
        .expect("{foo=bar}");
    request().get("/d")
        .expect("{}");
  }

}
