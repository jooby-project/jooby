package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue592 extends ServerFeature {

  public static enum State {
    Draft, Submitted;
  }

  {
    get("/592/:state", req -> req.param("state").toEnum(State.class));
    err((req, rsp, err) -> {
      rsp.send(err.getCause().getMessage());
    });
  }

  @Test
  public void casInsensitiveEnum() throws Exception {
    request()
        .get("/592/draft")
        .expect("Draft");

    request()
        .get("/592/SUBMITTED")
        .expect("Submitted");

    request()
        .get("/592/missing")
        .expect("No enum constant org.jooby.issues.Issue592.State.missing");
  }
}
