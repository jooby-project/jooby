package org.jooby.issues;

import org.jooby.Err;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue382 extends ServerFeature {

  {

    get("/err1", () -> {
      throw new IllegalArgumentException("err1");
    });

    get("/err2", () -> {
      throw new IllegalStateException("err2");
    });

    get("/400", () -> {
      throw new Err(404);
    });

    get("/500", () -> {
      throw new Err(500);
    });

    err(IllegalArgumentException.class, (req, rsp, err) -> {
      rsp.send(err.getCause().getClass().getSimpleName());
    });

    err(IllegalStateException.class, (req, rsp, err) -> {
      rsp.send(err.getCause().getClass().getSimpleName());
    });

    err(404, (req, rsp, err) -> {
      rsp.send("Not found");
    });

    err((req, rsp, err) -> {
      rsp.send("Fallback");
    });
  }

  @Test
  public void shouldHandleSpecificErrTypes() throws Exception {
    request()
        .get("/err1")
        .expect("IllegalArgumentException");

    request()
        .get("/err2")
        .expect("IllegalStateException");

    request()
        .get("/400")
        .expect("Not found");

    request()
        .get("/500")
        .expect("Fallback");
  }

}
