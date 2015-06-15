package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue128 extends ServerFeature {

  {
    put("/fake/put", req -> req.method());
  }

  @Test
  public void fakePutViaParamUrl() throws Exception {
    request().post("/fake/put?_method=put")
        .expect("PUT");
  }

  @Test
  public void fakePutViaFormParam() throws Exception {
    request().post("/fake/put")
        .form().add("_method", "PuT")
        .expect("PUT");
  }

  @Test
  public void fakePostViaHeader() throws Exception {
    request().post("/fake/put")
        .header("_method", "put")
        .expect("PUT");
  }

}
