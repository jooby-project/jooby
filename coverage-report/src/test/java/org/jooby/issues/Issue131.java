package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue131 extends ServerFeature {

  {

    assets("/swagger/ui/**", "/META-INF/resources/webjars/swagger-ui/2.1.8-M1/{0}");

  }

  @Test
  public void largeFileFromInputStream() throws Exception {
    request()
      .get("/swagger/ui/fonts/droid-sans-v6-latin-regular.woff")
      .expect(200)
      .header("Content-Length", 24868);
  }
}
