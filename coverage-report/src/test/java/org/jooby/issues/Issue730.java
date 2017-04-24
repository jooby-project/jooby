package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue730 extends ServerFeature {

  {
    get("/730", (req, rsp) -> {
      rsp.header("content-type", "application/json");
      rsp.send("{\"issue\": \"730\"}");
    });

  }

  @Test
  public void contentTypeShouldWorkFromSetHeader() throws Exception {
    request().get("/730")
        .execute()
        .header("Content-Type", "application/json;charset=utf-8");
  }

}
