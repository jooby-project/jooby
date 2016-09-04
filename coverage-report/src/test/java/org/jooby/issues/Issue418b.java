package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue418b extends ServerFeature {

  {

    get("/418b", req -> {
      req.push("/assets.js");
      return "OK";
    });
  }

  @Test
  public void pushUnsupportedOnHttp1_1() throws Exception {
    request()
    .get("/418b")
        .expect(500);
  }
}
