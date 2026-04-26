/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.jooby.MediaType;

public class URLAssetTest {

  @Test
  public void testUrlAssetMetadata() throws Exception {
    Path tempFile = Files.createTempFile("jooby-asset", ".txt");
    try {
      String content = "Hello Jooby!";
      Files.write(tempFile, content.getBytes());

      URL url = tempFile.toUri().toURL();
      URLAsset asset = new URLAsset(url, "foo/bar.txt");

      assertEquals("foo/bar.txt", asset.toString());
      assertEquals(MediaType.text, asset.getContentType());
      assertEquals(content.length(), asset.getSize());
      assertTrue(asset.getLastModified() > 0);
      assertFalse(asset.isDirectory());

      try (InputStream is = asset.stream()) {
        assertNotNull(is);
      }
      asset.close();
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }

  @Test
  public void testIsDirectory() throws Exception {
    // We create a physical empty file.
    // An empty file ALWAYS has a size of 0 across all OSs.
    // In URLAsset, getSize() == 0 returns true for isDirectory().
    Path emptyFile = Files.createTempFile("jooby-empty", ".bin");
    try {
      URL url = emptyFile.toUri().toURL();
      URLAsset asset = new URLAsset(url, "empty-file");

      assertEquals(0, asset.getSize());
      // This specifically triggers the branch: return getSize() == 0;
      assertTrue(asset.isDirectory());

      asset.close();
    } finally {
      Files.deleteIfExists(emptyFile);
    }
  }

  @Test
  public void testEqualsAndHashCode() throws Exception {
    URL url = new URL("file:///tmp/foo");
    URLAsset asset1 = new URLAsset(url, "path/a.txt");
    URLAsset asset2 = new URLAsset(url, "path/a.txt");
    URLAsset asset3 = new URLAsset(url, "path/b.txt");

    assertEquals(asset1, asset2);
    assertNotEquals(asset1, asset3);
    assertNotEquals(asset1, "not an asset");
    assertEquals(asset1.hashCode(), asset2.hashCode());
  }

  @Test
  public void testIOExceptionWrapping() throws Exception {
    URL url = new URL("file:///non/existent/file/path/jooby");
    URLAsset asset = new URLAsset(url, "badpath");

    assertThrows(FileNotFoundException.class, asset::getSize);
  }

  @Test
  public void testCloseHandling() {
    URLAsset asset = new URLAsset(null, "path");
    // Covers the null check in close()
    asset.close();
  }
}
