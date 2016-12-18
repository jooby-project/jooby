package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue596 extends ServerFeature {

  {
    get("/596", (req, rsp) -> {

    });
  }

  @Test
  public void shouldGet404onUnhandledRequests() throws Exception {
    request()
        .get("/596")
        .expect(404);
  }
}
