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
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.jooby.internal.openapi.OpenAPIExt;
import io.jooby.openapi.OpenAPIGenerator;
import io.swagger.v3.oas.models.info.Info;

public class Issue1582 {
  private Path outDir = Paths.get(System.getProperty("user.dir"), "target", "test-classes");

  @Test
  public void shouldGenerateOnOneLvelPackageLocation() throws IOException {
    var output = export("com.App");
    output.forEach(it -> assertTrue(Files.exists(it)));
    assertEquals(List.of(outDir.resolve("com").resolve("App.yaml")), output);
  }

  @Test
  public void shouldGenerateOnPackageLocation() throws IOException {
    var output = export("com.myapp.App");
    output.forEach(it -> assertTrue(Files.exists(it)));
    assertEquals(List.of(outDir.resolve("com").resolve("myapp").resolve("App.yaml")), output);
  }

  @Test
  public void shouldGenerateOnDeepPackageLocation() throws IOException {
    var output = export("com.foo.bar.app.App");
    output.forEach(it -> assertTrue(Files.exists(it)));
    assertEquals(
        List.of(
            outDir.resolve("com").resolve("foo").resolve("bar").resolve("app").resolve("App.yaml")),
        output);
  }

  @Test
  public void shouldGenerateOnRootLocation() throws IOException {
    var output = export("App");
    output.forEach(it -> assertTrue(Files.exists(it)));
    assertEquals(List.of(outDir.resolve("App.yaml")), output);
  }

  private List<Path> export(String source) throws IOException {
    Info info = new Info();
    info.setTitle("API");
    info.setVersion("1.0");
    info.setDescription("API description");

    OpenAPIExt openAPI = new OpenAPIExt();
    openAPI.setInfo(info);
    openAPI.setSource(source);

    OpenAPIGenerator generator = new OpenAPIGenerator();
    generator.setOutputDir(outDir);
    return generator.export(openAPI, OpenAPIGenerator.Format.YAML, Map.of());
  }
}
