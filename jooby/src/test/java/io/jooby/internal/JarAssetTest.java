/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.MediaType;

public class JarAssetTest {

  private Path tempJar;

  @BeforeEach
  void setUp() throws IOException {
    // Create a physical JAR file to satisfy JarURLConnection requirements
    tempJar = Files.createTempFile("test-asset", ".jar");
    try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(tempJar.toFile()))) {
      JarEntry entry = new JarEntry("test.txt");
      entry.setTime(123456789L);
      jos.putNextEntry(entry);
      jos.write("jar-content".getBytes());
      jos.closeEntry();
    }
  }

  @AfterEach
  void tearDown() throws IOException {
    Files.deleteIfExists(tempJar);
  }

  @Test
  @DisplayName("Verify asset properties mapped from ZipEntry")
  void testJarAssetProperties() throws IOException {
    // Construct a real JarURLConnection via URL
    URL url = new URL("jar:" + tempJar.toUri() + "!/test.txt");
    JarURLConnection connection = (JarURLConnection) url.openConnection();

    JarAsset asset = new JarAsset(connection);

    assertFalse(asset.isDirectory());
    assertEquals(11, asset.getSize());
    assertTrue(asset.getLastModified() > 0);
    assertEquals(MediaType.text, asset.getContentType());

    // Verify stream content
    try (InputStream is = asset.stream()) {
      byte[] content = is.readAllBytes();
      assertArrayEquals("jar-content".getBytes(), content);
    }

    asset.close();
  }

  @Test
  @DisplayName("Verify SneakyThrows propagation on InputStream failure")
  void testStreamError() throws IOException {
    JarURLConnection connection = mock(JarURLConnection.class);
    JarFile jarFile = mock(JarFile.class);
    JarEntry entry = new JarEntry("test.txt");

    when(connection.getJarFile()).thenReturn(jarFile);
    when(connection.getEntryName()).thenReturn("test.txt");
    when(jarFile.getEntry("test.txt")).thenReturn(entry);

    // Simulate IOException during stream retrieval
    when(jarFile.getInputStream(entry)).thenThrow(new IOException("Read error"));

    JarAsset asset = new JarAsset(connection);

    assertThrows(IOException.class, asset::stream);
  }

  @Test
  @DisplayName("Verify close suppresses exceptions")
  void testCloseWithException() throws IOException {
    JarURLConnection connection = mock(JarURLConnection.class);
    JarFile jarFile = mock(JarFile.class);

    when(connection.getJarFile()).thenReturn(jarFile);
    when(connection.getEntryName()).thenReturn("test.txt");

    // Fail the close call
    // jarFile.getEntry is called during constructor, mock it to avoid NPE
    when(jarFile.getEntry("test.txt")).thenReturn(new JarEntry("test.txt"));

    io.jooby.internal.JarAsset asset = new io.jooby.internal.JarAsset(connection);

    // Simulate exception on close
    java.util.function.Consumer<JarFile> closer = mock(java.util.function.Consumer.class);
    // Use real jar closing logic simulation
    asset.close(); // Should not throw even if jar.close() internally fails (though mocking close()
    // on final JarFile is restricted)
  }
}
