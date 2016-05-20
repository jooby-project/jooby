package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue375 extends ServerFeature {

  {
    get("/375", req -> req.cookie("X").value());
  }

  @Test
  public void shouldHandleOldCookieHeader() throws Exception {
    request()
        .get("/375")
        .header("Cookie", "X=x; $Version=1; $Path=/")
        .expect("x");
  }

}
