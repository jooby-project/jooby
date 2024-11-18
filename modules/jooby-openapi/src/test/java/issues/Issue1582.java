/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import io.jooby.internal.openapi.OpenAPIExt;
import io.jooby.openapi.OpenAPIGenerator;
import io.swagger.v3.oas.models.info.Info;

public class Issue1582 {
  private Path outDir = Paths.get(System.getProperty("user.dir"), "target", "test-classes");

  @Test
  public void shouldGenerateOnOneLvelPackageLocation() throws IOException {
    Path output = export("com.App");
    assertTrue(Files.exists(output));
    assertEquals(outDir.resolve("com").resolve("App.yaml"), output);
  }

  @Test
  public void shouldGenerateOnPackageLocation() throws IOException {
    Path output = export("com.myapp.App");
    assertTrue(Files.exists(output));
    assertEquals(outDir.resolve("com").resolve("myapp").resolve("App.yaml"), output);
  }

  @Test
  public void shouldGenerateOnDeepPackageLocation() throws IOException {
    Path output = export("com.foo.bar.app.App");
    assertTrue(Files.exists(output));
    assertEquals(
        outDir.resolve("com").resolve("foo").resolve("bar").resolve("app").resolve("App.yaml"),
        output);
  }

  @Test
  public void shouldGenerateOnRootLocation() throws IOException {
    Path output = export("App");
    assertTrue(Files.exists(output));
    assertEquals(outDir.resolve("App.yaml"), output);
  }

  private Path export(String source) throws IOException {
    Info info = new Info();
    info.setTitle("API");
    info.setVersion("1.0");
    info.setDescription("API description");

    OpenAPIExt openAPI = new OpenAPIExt();
    openAPI.setInfo(info);
    openAPI.setSource(source);

    OpenAPIGenerator generator = new OpenAPIGenerator();
    generator.setOutputDir(outDir);
    return generator.export(openAPI, OpenAPIGenerator.Format.YAML);
  }
}
