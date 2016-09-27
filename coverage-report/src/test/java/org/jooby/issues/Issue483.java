package org.jooby.issues;

import java.util.Optional;

import org.jooby.Parser;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue483 extends ServerFeature {

  public static class NullableBean {

    String foo;

    Optional<String> bar;

    @Override
    public String toString() {
      return foo + bar;
    }
  }

  {
    parser(Parser.bean(true));

    get("/483", req -> {
      return req.params(NullableBean.class).toString();
    });

  }

  @Test
  public void documentNullBeanInjection() throws Exception {
    request()
        .get("/483?foo=foo")
        .expect("foonull");

    request()
        .get("/483?foo=foo&bar")
        .expect("fooOptional[]");

    request()
        .get("/483?foo=foo&bar=bar")
        .expect("fooOptional[bar]");
  }

}
