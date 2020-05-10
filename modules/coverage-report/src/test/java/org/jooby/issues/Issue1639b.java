package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import java.nio.file.Paths;

public class Issue1639b extends ServerFeature {

  {
    assets("/static/**/*.js", Paths.get("static"));
  }

  @Test
  public void shouldNotByPassPrefixValue() throws Exception {
    request()
        .get("/static/org/jooby/issues/Issue1639b.class.js")
        .expect(404);

    request()
        .get("/static/../org/jooby/issues/Issue1639b.class.js")
        .expect(404);

    request()
        .get("/static/..%252forg/jooby/issues/Issue1639b.class.js")
        .expect(404);

    request()
        .get("/static/org/jooby/issues/Issue1639b.class")
        .expect(404);
  }

}
