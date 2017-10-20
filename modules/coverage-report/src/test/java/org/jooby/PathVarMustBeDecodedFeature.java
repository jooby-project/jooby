package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class PathVarMustBeDecodedFeature extends ServerFeature {

  {
    get("/:var", req -> req.param("var").value());
  }

  @Test
  public void pathVarShouldBeDecoded() throws Exception {
    request()
        .get("/path%20with%20spaces")
        .expect("path with spaces");

    request()
        .get("/plus+plus")
        .expect("plus+plus");
  }
}
