package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue30 extends ServerFeature {

  {

    get("/def-end", (req, rsp) -> {
      rsp.status(200);
    });

    get("/force", (req, rsp) -> {
      rsp.end();
    });

    get("/force", (req, rsp) -> {
      throw new IllegalStateException("Should never get here");
    });
  }

  @Test
  public void defEnd() throws Exception {
    request()
        .get("/def-end")
        .expect(200)
        .header("Content-Length", 0)
        .empty();
  }

  @Test
  public void force() throws Exception {
    request()
        .get("/force")
        .expect(200)
        .header("Content-Length", 0)
        .empty();
  }

}
