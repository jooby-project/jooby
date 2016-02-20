package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue279 extends ServerFeature {

  {
    use("*", (req, rsp) -> {
      req.set("local", 678);
    });

    use("/issue279")
        .get("/get", req -> {
          int value = req.get("local");
          return value;
        })
        .get("/getdef", req -> {
          String value = req.get("def", "def");
          return value;
        })
        .get("/geterr", req -> {
          String value = req.get("def");
          return value;
        });
  }

  @Test
  public void getRequestLocals() throws Exception {
    request().get("/issue279/get").expect("678");

    request().get("/issue279/getdef").expect("def");

    request().get("/issue279/geterr").expect(400);
  }

}
