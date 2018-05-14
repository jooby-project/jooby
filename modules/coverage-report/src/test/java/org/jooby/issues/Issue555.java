package org.jooby.issues;

import org.jooby.assets.AssetsBase;
import org.junit.Test;

public class Issue555 extends AssetsBase {

  {
    get("/555/raw/**", req -> req.rawPath());
    get("/555/path/**", req -> req.path());
  }

  @Test
  public void rawPath() throws Exception {
    request()
        .get("/555/raw/x%252Fy%252Fz")
        .expect("/555/raw/x%252Fy%252Fz");

    request()
        .get("/555/path/x%252Fy%252Fz")
        .expect("/555/path/x/y/z");
  }

}
