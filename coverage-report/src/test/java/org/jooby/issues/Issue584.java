package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue584 extends ServerFeature {

  {
  }

  @Test
  public void silentFaviconRequest() throws Exception {
    request()
        .get("/favicon.ico")
        .expect(404)
        .expect("");
  }
}
