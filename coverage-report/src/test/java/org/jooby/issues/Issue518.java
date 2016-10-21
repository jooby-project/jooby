package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue518 extends ServerFeature {

  {
    get("/518", req -> {
      return req.path() + req.queryString().map(it -> "?" + it).orElse("");
    });
  }

  @Test
  public void shouldGetQueryString() throws Exception {
    request()
        .get("/518?foo=1&bar=2&baz=3")
        .expect("/518?foo=1&bar=2&baz=3");

    request()
        .get("/518")
        .expect("/518");

    request()
        .get("/518?")
        .expect("/518");
  }

}
