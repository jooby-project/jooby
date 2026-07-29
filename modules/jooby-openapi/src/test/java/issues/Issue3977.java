/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues;

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

public class Issue3977 {
  private Path outDir = Paths.get(System.getProperty("user.dir"), "target", "test-classes");

  @Test
  public void shouldCopyYamlToDir() throws IOException {
    var copyDir = Paths.get(System.getProperty("user.dir"), "target", "spec");
    var output = export("App", OpenAPIGenerator.Format.YAML, List.of(copyDir));
    output.forEach(it -> assertTrue(Files.exists(it)));
    assertTrue(output.contains(copyDir.resolve("App.yaml")));
  }

  @Test
  public void shouldCopyJsonToDir() throws IOException {
    var copyDir = Paths.get(System.getProperty("user.dir"), "target", "spec");
    var output = export("App", OpenAPIGenerator.Format.JSON, List.of(copyDir));
    output.forEach(it -> assertTrue(Files.exists(it)));
    assertTrue(output.contains(copyDir.resolve("App.json")));
  }

  @Test
  public void shouldCopyJsonFile() throws IOException {
    var copyFile = Paths.get(System.getProperty("user.dir"), "target", "files", "open-api.json");
    var output = export("App", OpenAPIGenerator.Format.JSON, List.of(copyFile));
    output.forEach(it -> assertTrue(Files.exists(it)));
    assertTrue(output.contains(copyFile));
  }

  @Test
  public void shouldCopyYamlFile() throws IOException {
    var copyFile = Paths.get(System.getProperty("user.dir"), "target", "files", "open-api.yaml");
    var output = export("App", OpenAPIGenerator.Format.YAML, List.of(copyFile));
    output.forEach(it -> assertTrue(Files.exists(it)));
    assertTrue(output.contains(copyFile));
  }

  @Test
  public void shouldCopyYmlFile() throws IOException {
    var copyFile = Paths.get(System.getProperty("user.dir"), "target", "files", "open-api.yml");
    var output = export("App", OpenAPIGenerator.Format.YAML, List.of(copyFile));
    output.forEach(it -> assertTrue(Files.exists(it)));
    assertTrue(output.contains(copyFile));
  }

  private List<Path> export(String source, OpenAPIGenerator.Format format, List<Path> copySpecTo)
      throws IOException {
    Info info = new Info();
    info.setTitle("API");
    info.setVersion("1.0");
    info.setDescription("API description");

    OpenAPIExt openAPI = new OpenAPIExt();
    openAPI.setInfo(info);
    openAPI.setSource(source);

    OpenAPIGenerator generator = new OpenAPIGenerator();
    generator.setOutputDir(outDir);
    generator.setCopyOpenApiSpecTo(copySpecTo);
    return generator.export(openAPI, format, Map.of());
  }
}
