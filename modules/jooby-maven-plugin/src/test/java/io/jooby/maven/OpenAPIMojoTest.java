package io.jooby.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.jooby.openapi.OpenAPIGenerator;

public class OpenAPIMojoTest {

  @Test
  public void specFormat() {
    assertEquals(
        OpenAPIGenerator.Format.YAML, OpenAPIMojo.specFormat(Path.of("docs/openapi.yaml")));
    assertEquals(
        OpenAPIGenerator.Format.YAML, OpenAPIMojo.specFormat(Path.of("docs/openapi.yml")));
    assertEquals(
        OpenAPIGenerator.Format.JSON, OpenAPIMojo.specFormat(Path.of("docs/openapi.json")));
    assertThrows(
        IllegalArgumentException.class, () -> OpenAPIMojo.specFormat(Path.of("docs/openapi.txt")));
  }

  @Test
  public void copyYamlSpec(@TempDir Path tempDir) throws Exception {
    var outputDir = tempDir.resolve("classes/myapp");
    Files.createDirectories(outputDir);
    var source = outputDir.resolve("App.yaml");
    Files.writeString(source, "openapi: 3.0.1");

    var destination = tempDir.resolve("docs/openapi.yml");
    OpenAPIMojo.copySpec(List.of(source), destination);

    assertTrue(Files.isRegularFile(destination));
    assertEquals(Files.readString(source), Files.readString(destination));
  }

  @Test
  public void copyJsonSpec(@TempDir Path tempDir) throws Exception {
    var outputDir = tempDir.resolve("classes/myapp");
    Files.createDirectories(outputDir);
    var source = outputDir.resolve("App.json");
    Files.writeString(source, "{\"openapi\":\"3.0.1\"}");

    var destination = tempDir.resolve("docs/openapi.json");
    OpenAPIMojo.copySpec(List.of(source), destination);

    assertTrue(Files.isRegularFile(destination));
    assertEquals(Files.readString(source), Files.readString(destination));
  }
}
