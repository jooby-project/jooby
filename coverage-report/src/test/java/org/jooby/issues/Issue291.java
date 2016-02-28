package org.jooby.issues;

import org.jooby.Renderer;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue291 extends ServerFeature {

  {
    renderer(Renderer.of("bin", (v, c) -> c.send(Integer.toBinaryString((Integer) v))));

    get("/issue291", () -> 2).renderer("bin");
  }

  @Test
  public void shouldNotDisplayStacktrace() throws Exception {
    request().get("/issue291")
        .expect("10");
  }

}
