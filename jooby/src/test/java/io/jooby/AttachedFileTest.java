/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class AttachedFileTest {

  @Test
  public void testInputStreamConstructors() throws IOException {
    byte[] data = "content".getBytes();

    // Test: InputStream, fileName, fileSize
    try (var is = new ByteArrayInputStream(data)) {
      AttachedFile file = new AttachedFile(is, "test1.txt", data.length);
      assertEquals("test1.txt", file.getFileName());
      assertEquals(data.length, file.getFileSize());
      assertTrue(file.getContentDisposition().startsWith("attachment"));
    }

    // Test: InputStream, fileName
    try (var is = new ByteArrayInputStream(data)) {
      AttachedFile file = new AttachedFile(is, "test2.txt");
      assertEquals("test2.txt", file.getFileName());
      assertEquals(-1, file.getFileSize());
      assertTrue(file.getContentDisposition().startsWith("attachment"));
    }
  }

  @Test
  public void testPathConstructors() throws IOException {
    Path tempFile = Files.createTempFile("attached-file", ".json");
    Files.write(tempFile, "{}".getBytes());

    try {
      // Test: Path, fileName
      AttachedFile f1 = new AttachedFile(tempFile, "custom.json");
      assertEquals("custom.json", f1.getFileName());
      assertEquals(2, f1.getFileSize());
      assertTrue(f1.getContentDisposition().contains("attachment"));
      f1.stream().close();

      // Test: Path
      AttachedFile f2 = new AttachedFile(tempFile);
      assertEquals(tempFile.getFileName().toString(), f2.getFileName());
      assertTrue(f2.getContentDisposition().contains("attachment"));
      f2.stream().close();
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }
}
