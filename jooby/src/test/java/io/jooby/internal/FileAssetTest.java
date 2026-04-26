/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jooby.MediaType;

public class FileAssetTest {

  private Path tempFile;
  private FileAsset asset;

  @BeforeEach
  void setUp() throws IOException {
    tempFile = Files.createTempFile("jooby-asset", ".txt");
    Files.writeString(tempFile, "asset-content");
    asset = new FileAsset(tempFile);
  }

  @AfterEach
  void tearDown() throws IOException {
    Files.deleteIfExists(tempFile);
  }

  @Test
  void testMetadata() throws IOException {
    assertEquals(Files.size(tempFile), asset.getSize());
    assertEquals(Files.getLastModifiedTime(tempFile).toMillis(), asset.getLastModified());
    assertEquals(MediaType.text, asset.getContentType());
    assertFalse(asset.isDirectory());
    assertEquals(tempFile.toString(), asset.toString());
  }

  @Test
  void testStream() throws IOException {
    try (InputStream is = asset.stream()) {
      assertNotNull(is);
      assertEquals("asset-content", new String(is.readAllBytes()));
    }
  }

  @Test
  void testDirectory() throws IOException {
    Path dir = Files.createTempDirectory("jooby-dir");
    try {
      FileAsset dirAsset = new FileAsset(dir);
      assertTrue(dirAsset.isDirectory());
    } finally {
      Files.deleteIfExists(dir);
    }
  }

  @Test
  void testEqualsAndHashCode() {
    FileAsset same = new FileAsset(tempFile);
    FileAsset different = new FileAsset(Paths.get("other-file.txt"));

    assertEquals(asset, same);
    assertEquals(asset.hashCode(), same.hashCode());
    assertNotEquals(asset, different);
    assertNotEquals(asset, "not-an-asset");
  }

  @Test
  void testClose() {
    // Should be a NOOP
    asset.close();
  }

  @Test
  void testIOExceptions() throws IOException {
    // Delete file to trigger IOExceptions on existing asset
    Files.deleteIfExists(tempFile);

    assertThrows(NoSuchFileException.class, () -> asset.getSize());
    assertThrows(NoSuchFileException.class, () -> asset.getLastModified());
    assertThrows(FileNotFoundException.class, () -> asset.stream());
  }
}
