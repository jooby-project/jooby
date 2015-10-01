package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue198 extends ServerFeature {

  {

    assets("/js/**", "/META-INF/resources/webjars/{0}");

  }

  @Test
  public void oddpath() throws Exception {
    request()
      .get("/js/jquery")
      .expect(404);
  }
}
