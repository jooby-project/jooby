/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.jar.JarFile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.jooby.MediaType;

public class AssetTest {

  @Test
  @DisplayName("Verify create(Path) returns a FileAsset")
  void testCreateFromPath(@TempDir Path tempDir) {
    Asset asset = Asset.create(tempDir);
    assertNotNull(asset);
    assertEquals("FileAsset", asset.getClass().getSimpleName());
  }

  @Test
  @DisplayName("Verify create(String, URL) handles 'jar' protocol")
  void testCreateFromUrlJarProtocol() throws Exception {
    URL url = mock(URL.class);
    when(url.getProtocol()).thenReturn("jar");

    JarURLConnection connection = mock(JarURLConnection.class);
    JarFile jarFile = mock(JarFile.class);

    // Fix: Provide a mocked JarFile so JarAsset constructor doesn't throw NPE
    when(connection.getJarFile()).thenReturn(jarFile);
    when(url.openConnection()).thenReturn(connection);

    Asset asset = Asset.create("/path", url);
    assertNotNull(asset);
    assertEquals("JarAsset", asset.getClass().getSimpleName());
  }

  @Test
  @DisplayName("Verify create(String, URL) handles 'file' protocol")
  void testCreateFromUrlFileProtocol() throws Exception {
    URL url = mock(URL.class);
    when(url.getProtocol()).thenReturn("file");
    when(url.toURI()).thenReturn(new URI("file:///tmp/dummy-file.txt"));

    Asset asset = Asset.create("/path", url);
    assertNotNull(asset);
    assertEquals("FileAsset", asset.getClass().getSimpleName());
  }

  @Test
  @DisplayName("Verify create(String, URL) handles other standard protocols (e.g., http)")
  void testCreateFromUrlOtherProtocol() throws Exception {
    URL url = mock(URL.class);
    when(url.getProtocol()).thenReturn("http");

    Asset asset = Asset.create("/path", url);
    assertNotNull(asset);
    assertEquals("URLAsset", asset.getClass().getSimpleName());
  }

  @Test
  @DisplayName("Verify create(String, URL) throws SneakyThrows on IOException")
  void testCreateFromUrlThrowsIOException() throws Exception {
    URL url = mock(URL.class);
    when(url.getProtocol()).thenReturn("jar");
    when(url.openConnection()).thenThrow(new IOException("Connection failed"));

    // SneakyThrows propagates the original checked exception as unchecked
    IOException thrown = assertThrows(IOException.class, () -> Asset.create("/path", url));
    assertEquals("Connection failed", thrown.getMessage());
  }

  @Test
  @DisplayName("Verify create(String, URL) throws SneakyThrows on URISyntaxException")
  void testCreateFromUrlThrowsURISyntaxException() throws Exception {
    URL url = mock(URL.class);
    when(url.getProtocol()).thenReturn("file");
    when(url.toURI()).thenThrow(new URISyntaxException("invalid-uri", "Syntax error"));

    URISyntaxException thrown =
        assertThrows(URISyntaxException.class, () -> Asset.create("/path", url));
    assertEquals("Syntax error", thrown.getReason());
  }

  @Test
  @DisplayName("Verify getEtag computes a valid weak ETag string")
  void testGetEtag() {
    // Implement an anonymous Asset to test the default method behavior
    Asset asset =
        new Asset() {
          @Override
          public long getSize() {
            return 2048L;
          }

          @Override
          public long getLastModified() {
            return 1625097600000L; // Static epoch time for consistency
          }

          @Override
          public boolean isDirectory() {
            return false;
          }

          @Override
          public MediaType getContentType() {
            return MediaType.text;
          }

          @Override
          public InputStream stream() {
            return null; // Not needed for ETag
          }

          @Override
          public void close() {
            // NOOP
          }

          @Override
          public int hashCode() {
            return 123456; // Stable hash code for predictability
          }
        };

    String etag = asset.getEtag();

    assertNotNull(etag);
    assertTrue(etag.startsWith("W/\""), "ETag should start with the weak validator prefix W/\"");
    assertTrue(etag.endsWith("\""), "ETag should end with a quote");
  }
}
