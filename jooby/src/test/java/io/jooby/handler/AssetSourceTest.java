/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AssetSourceTest {

  @Test
  @DisplayName("Verify create(ClassLoader, String) returns ClassPathAssetSource")
  void testCreateClasspathSource() {
    ClassLoader loader = mock(ClassLoader.class);
    AssetSource source = AssetSource.create(loader, "/static");

    assertNotNull(source);
    assertEquals("ClassPathAssetSource", source.getClass().getSimpleName());
  }

  @Test
  @DisplayName("Verify webjars standard Maven path resolution")
  void testWebjarsMavenResolution() throws Exception {
    ClassLoader loader = mock(ClassLoader.class);
    String name = "swagger-ui";
    String pomPath = "META-INF/maven/org.webjars/" + name + "/pom.properties";

    when(loader.getResource(pomPath)).thenReturn(new URL("file://dummy"));
    when(loader.getResourceAsStream(pomPath))
        .thenReturn(new ByteArrayInputStream("version=3.0.0".getBytes(StandardCharsets.UTF_8)));

    AssetSource source = AssetSource.webjars(loader, name);
    assertEquals("ClassPathAssetSource", source.getClass().getSimpleName());
  }

  @Test
  @DisplayName("Verify webjars NPM path resolution fallback")
  void testWebjarsNpmResolution() throws Exception {
    ClassLoader loader = mock(ClassLoader.class);
    String name = "vue";
    String mavenPath = "META-INF/maven/org.webjars/" + name + "/pom.properties";
    String npmPath = "META-INF/maven/org.webjars.npm/" + name + "/pom.properties";

    // Maven path not found, NPM path found
    when(loader.getResource(mavenPath)).thenReturn(null);
    when(loader.getResource(npmPath)).thenReturn(new URL("file://dummy"));

    when(loader.getResourceAsStream(npmPath))
        .thenReturn(new ByteArrayInputStream("version=2.6.11".getBytes(StandardCharsets.UTF_8)));

    AssetSource source = AssetSource.webjars(loader, name);
    assertEquals("ClassPathAssetSource", source.getClass().getSimpleName());
  }

  @Test
  @DisplayName(
      "Verify webjars throws SneakyThrows wrapped FileNotFoundException when pom is missing")
  void testWebjarsNotFound() {
    ClassLoader loader = mock(ClassLoader.class);
    // getResource returns null for all paths

    assertThrows(FileNotFoundException.class, () -> AssetSource.webjars(loader, "missing-lib"));
  }

  @Test
  @DisplayName("Verify webjars throws SneakyThrows wrapped IOException on stream failure")
  void testWebjarsIOException() throws Exception {
    ClassLoader loader = mock(ClassLoader.class);
    String name = "broken-lib";
    String pomPath = "META-INF/maven/org.webjars/" + name + "/pom.properties";

    when(loader.getResource(pomPath)).thenReturn(new URL("file://dummy"));

    // Create a stream that throws IOException when properties.load() tries to read it
    InputStream badStream =
        new InputStream() {
          @Override
          public int read() throws IOException {
            throw new IOException("Forced read error");
          }
        };
    when(loader.getResourceAsStream(anyString())).thenReturn(badStream);

    assertThrows(IOException.class, () -> AssetSource.webjars(loader, name));
  }

  @Test
  @DisplayName("Verify create(Path) returns FolderDiskAssetSource for directories")
  void testCreateFolderSource(@TempDir Path tempDir) {
    AssetSource source = AssetSource.create(tempDir);

    assertNotNull(source);
    assertEquals("FolderDiskAssetSource", source.getClass().getSimpleName());
  }

  @Test
  @DisplayName("Verify create(Path) returns FileDiskAssetSource for standard files")
  void testCreateFileSource(@TempDir Path tempDir) throws IOException {
    Path tempFile = Files.createFile(tempDir.resolve("asset.txt"));

    AssetSource source = AssetSource.create(tempFile);

    assertNotNull(source);
    assertEquals("FileDiskAssetSource", source.getClass().getSimpleName());
  }

  @Test
  @DisplayName(
      "Verify create(Path) throws SneakyThrows wrapped FileNotFoundException for non-existent"
          + " paths")
  void testCreatePathNotFound(@TempDir Path tempDir) {
    Path nonExistent = tempDir.resolve("does-not-exist.txt");

    assertThrows(FileNotFoundException.class, () -> AssetSource.create(nonExistent));
  }
}
