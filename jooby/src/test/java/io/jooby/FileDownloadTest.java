/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class FileDownloadTest {

  @Test
  public void testBasicProperties() {
    byte[] content = "hello".getBytes();
    FileDownload download = new FileDownload(FileDownload.Mode.ATTACHMENT, content, "test.txt");

    assertEquals("test.txt", download.getFileName());
    assertEquals("test.txt", download.toString());
    assertEquals(5, download.getFileSize());
    assertEquals(MediaType.text, download.getContentType());
    assertEquals("attachment;filename=\"test.txt\"", download.getContentDisposition());
    assertNotNull(download.stream());
    assertNull(download.getFile());
  }

  @Test
  public void testFilenameStarEncoding() {
    // Testing a filename with spaces and non-ASCII characters to trigger filename* logic
    String name = "my file 🚀.txt";
    FileDownload download =
        new FileDownload(FileDownload.Mode.INLINE, new ByteArrayInputStream(new byte[0]), name);

    // Should contain the standard filename and the encoded filename*
    String disposition = download.getContentDisposition();
    assertTrue(disposition.startsWith("inline;filename=\"my file 🚀.txt\""));
    assertTrue(disposition.contains(";filename*=UTF-8''my%20file%20%F0%9F%9A%80.txt"));
  }

  @Test
  public void testPathConstructors() throws IOException {
    Path file = Files.createTempFile("jooby-download", ".json");
    Files.write(file, "{}".getBytes());

    try {
      // Constructor with Path and Name
      FileDownload d1 = new FileDownload(FileDownload.Mode.ATTACHMENT, file, "custom.json");
      assertEquals("custom.json", d1.getFileName());
      assertEquals(2, d1.getFileSize());
      assertEquals(file, d1.getFile());

      // Constructor with Path only
      FileDownload d2 = new FileDownload(FileDownload.Mode.INLINE, file);
      assertEquals(file.getFileName().toString(), d2.getFileName());

      d1.stream().close();
      d2.stream().close();
    } finally {
      Files.deleteIfExists(file);
    }
  }

  @Test
  public void testBuilders() {
    // InputStream Builder
    InputStream stream = new ByteArrayInputStream(new byte[10]);
    FileDownload d1 = FileDownload.build(stream, "file.bin", 10).attachment();
    assertEquals(FileDownload.Mode.ATTACHMENT.value, d1.getContentDisposition().split(";")[0]);

    // byte[] Builder
    FileDownload d2 = FileDownload.build("data".getBytes(), "data.txt").inline();
    assertEquals(FileDownload.Mode.INLINE.value, d2.getContentDisposition().split(";")[0]);

    // InputStream without size
    FileDownload d3 =
        FileDownload.build(new ByteArrayInputStream(new byte[0]), "nosize.txt").attachment();
    assertEquals(-1, d3.getFileSize());
  }

  @Test
  public void testBuilderExtWithPath() throws IOException {
    Path file = Files.createTempFile("delete-test", ".txt");
    try {
      // Test BuilderExt features: Path only and deleteOnComplete
      FileDownload download =
          FileDownload.build(file).deleteOnComplete().build(FileDownload.Mode.ATTACHMENT);

      assertTrue(download.deleteOnComplete());
      assertEquals(file, download.getFile());
      download.stream().close();

      // Test BuilderExt with custom name
      FileDownload d2 = FileDownload.build(file, "renamed.txt").attachment();
      assertEquals("renamed.txt", d2.getFileName());
      d2.stream().close();
    } finally {
      Files.deleteIfExists(file);
    }
  }

  @Test
  public void testBuilderError() {
    // Attempt to build from a non-existent path to trigger IOException -> SneakyThrows
    Path nonExistent = java.nio.file.Paths.get("non", "existent", "path", "to", "file");
    FileDownload.BuilderExt builder = FileDownload.build(nonExistent);

    assertThrows(FileNotFoundException.class, () -> builder.attachment());
  }
}
