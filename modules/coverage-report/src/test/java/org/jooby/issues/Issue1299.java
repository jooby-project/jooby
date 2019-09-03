package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue1299 extends ServerFeature {

  {
    use("*", (req, rsp) -> {
      if (req.param("noreset").booleanValue(false)) {
        rsp.setResetHeadersOnError(false);
      }
    });
    get("/1299", req -> {
      req.flash("error", "error");
      return req.param("missing").value();
    });

    err((req, rsp, x) -> {
      rsp.status(200).end();
    });
  }

  @Test
  public void shouldNotResetHeaders() throws Exception {
    request()
        .get("/1299")
        .expect(200)
        .header("Set-Cookie", (String) null);
  }

}
