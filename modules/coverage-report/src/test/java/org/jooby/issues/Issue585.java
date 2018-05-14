package org.jooby.issues;

import java.nio.file.Paths;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue585 extends ServerFeature {

  {
    assets("/static/**", Paths.get("src/test/resources/static"));
  }

  @Test
  public void shouldServeStaticFiles() throws Exception {
    request()
        .get("/static/images/fun.gif")
        .expect(200)
        .header("Content-Length", "79530");

    request()
        .get("/static/images/prey.jpg")
        .expect(200)
        .header("Content-Length", "39003");
  }

  @Test
  public void shouldNotAllowAccessToResourceOutsideScope() throws Exception {
    request()
        .get("/static/../forbidden.txt")
        .expect(404);
  }
}
