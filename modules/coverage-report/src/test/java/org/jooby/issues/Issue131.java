package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue131 extends ServerFeature {

  {

    assets("/swagger/ui/**", "/META-INF/resources/webjars/swagger-ui/3.9.3/{0}");

  }

  @Test
  public void largeFileFromInputStream() throws Exception {
    request()
        .get("/swagger/ui/swagger-ui.js")
        .expect(200)
        .header("Content-Length", 349262);
  }
}
