package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue407 extends ServerFeature {

  {
    get("/407", req -> req.param("foo").toOptional());
  }

  @Test
  public void shouldNotIgnoreEmptyParams() throws Exception {
    request()
        .get("/407")
        .expect("Optional.empty");

    request()
        .get("/407?foo")
        .expect("Optional[]");

    request()
        .get("/407?foo=")
        .expect("Optional[]");
  }

}
