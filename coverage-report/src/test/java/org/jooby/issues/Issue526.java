package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue526 extends ServerFeature {

  {
    get("/526/V{var:\\d{4,7}}", req -> req.param("var").value());

    get("/526/var/:var", req -> req.param("var").value());

    err((req, rsp, x) -> {
      rsp.send(x.getMessage());
    });
  }

  public void shouldAcceptAdvancedRegexPathExpression() throws Exception {
    request()
        .get("/526/V1234")
        .expect("1234");

    request()
        .get("/526/V12")
        .expect(404);

    request()
        .get("/526/V1234567")
        .expect("1234567");

    request()
        .get("/526/V12345678")
        .expect(404);
  }

  @Test
  public void shouldAcceptSpecialChars() throws Exception {
    request()
        .get("/526/var/x%252Fy%252Fz")
        .expect("Not Found(404): /526/var/x/y/z");
  }

}
