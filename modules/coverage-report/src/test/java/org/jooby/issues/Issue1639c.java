package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import java.nio.file.Paths;

public class Issue1639c extends ServerFeature {

  {
    assets("/static/**");
  }

  @Test
  public void shouldNotByPassPrefixValue() throws Exception {
    request()
        .get("/static/..%252forg/jooby/issues/Issue1639c.class")
        .expect(404);
    request()
        .get("/static/../org/jooby/issues/Issue1639c.class")
        .expect(404);
  }

}
