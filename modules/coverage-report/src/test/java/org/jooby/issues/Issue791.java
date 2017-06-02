package org.jooby.issues;

import org.jooby.Err;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue791 extends ServerFeature {

  {
    get("/791", req -> req.param("missingParam").value());

    err(Err.Missing.class, (req, rsp, x) -> {
      rsp.send("Missing");
    });
  }

  @Test
  public void specializedErrHandlerShouldCatchErrExceptions() throws Exception {
    request()
        .get("/791")
        .expect("Missing");
  }

}
