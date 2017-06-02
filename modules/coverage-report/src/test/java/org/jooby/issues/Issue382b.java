package org.jooby.issues;

import static javaslang.Predicates.is;

import org.jooby.Err;
import org.jooby.Status;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue382b extends ServerFeature {

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

    err(Status.NOT_FOUND, (req, rsp, err) -> {
      rsp.send("Not found");
    });

    err(is(Status.BAD_REQUEST), (req, rsp, err) -> {
      rsp.send(err.getCause().getClass().getSimpleName());
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
        .expect("Fallback");

    request()
        .get("/400")
        .expect("Not found");

    request()
        .get("/500")
        .expect("Fallback");
  }

}
