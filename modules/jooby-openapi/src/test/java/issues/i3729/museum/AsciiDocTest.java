/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3729.museum;

import static io.swagger.v3.oas.models.SpecVersion.V31;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

public class AsciiDocTest {

  @OpenAPITest(value = MuseumApp.class, version = V31)
  public void shouldGenerateDoc(OpenAPIResult result) {
    assertEquals("", result.toAsciiDoc(basedir().resolve("adoc").resolve("museum.adoc")));
  }

  private static Path basedir() {
    var baseDir = Paths.get(System.getProperty("user.dir"));
    if (!baseDir.getFileName().toString().endsWith("jooby-openapi")) {
      baseDir = baseDir.resolve("modules").resolve("jooby-openapi");
    }
    return baseDir.resolve("src").resolve("test").resolve("resources");
  }
}
