package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class Issue1639 extends ServerFeature {

  {
    Path dir = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "assets");
    assertTrue(Files.exists(dir));
    assets("/static/**", dir);
  }

  @Test
  public void shouldNotFallbackToArbitraryClasspathResources() throws Exception {
    request()
        .get("/static/WEB-INF/web2.xml")
        .expect(404);

    request()
        .get("/static/../WEB-INF/web2.xml")
        .expect(404);
  }

}
