/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.MultiPart;
import org.eclipse.jetty.io.Content;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jooby.SneakyThrows;

@ExtendWith(MockitoExtension.class)
class JettyFileUploadTest {

  @Mock Path tmpdir;
  @Mock MultiPart.Part upload;

  private JettyFileUpload fileUpload;

  @BeforeEach
  void setup() {
    fileUpload = new JettyFileUpload(tmpdir, upload);
  }

  @Test
  void testGetName() {
    when(upload.getName()).thenReturn("avatar");
    assertEquals("avatar", fileUpload.getName());
  }

  @Test
  void testGetFileName() {
    when(upload.getFileName()).thenReturn("profile.png");
    assertEquals("profile.png", fileUpload.getFileName());
  }

  @Test
  void testToString() {
    when(upload.getFileName()).thenReturn("profile.png");
    assertEquals("profile.png", fileUpload.toString());
  }

  @Test
  void testGetFileSize() {
    when(upload.getLength()).thenReturn(1024L);
    assertEquals(1024L, fileUpload.getFileSize());
  }

  @Test
  void testGetContentType() {
    HttpFields headers = mock(HttpFields.class);
    when(upload.getHeaders()).thenReturn(headers);
    when(headers.get(HttpHeader.CONTENT_TYPE)).thenReturn("image/png");

    assertEquals("image/png", fileUpload.getContentType());
  }

  @Test
  void testClose() {
    fileUpload.close();
    verify(upload).close();
  }

  @Test
  void testStream_Success() {
    Content.Source contentSource = mock(Content.Source.class);
    when(upload.getContentSource()).thenReturn(contentSource);
    InputStream mockStream = mock(InputStream.class);

    try (MockedStatic<Content.Source> sourceStatic = mockStatic(Content.Source.class)) {
      sourceStatic.when(() -> Content.Source.asInputStream(contentSource)).thenReturn(mockStream);

      assertEquals(mockStream, fileUpload.stream());
    }
  }

  @Test
  void testStream_ReturnsNullOnException() {
    // If getting the content source fails, the method gracefully returns null
    when(upload.getContentSource()).thenThrow(new RuntimeException("Source unavailable"));

    assertNull(fileUpload.stream());
  }

  @Test
  void testBytes_Success() {
    byte[] expectedData = {10, 20, 30};
    InputStream mockStream = new ByteArrayInputStream(expectedData);
    Content.Source contentSource = mock(Content.Source.class);
    when(upload.getContentSource()).thenReturn(contentSource);

    try (MockedStatic<Content.Source> sourceStatic = mockStatic(Content.Source.class)) {
      sourceStatic.when(() -> Content.Source.asInputStream(contentSource)).thenReturn(mockStream);

      assertArrayEquals(expectedData, fileUpload.bytes());
    }
  }

  @Test
  void testBytes_ThrowsException() {
    InputStream failingStream =
        new InputStream() {
          @Override
          public int read() throws IOException {
            throw new IOException("Stream read failed");
          }
        };

    Content.Source contentSource = mock(Content.Source.class);
    when(upload.getContentSource()).thenReturn(contentSource);

    try (MockedStatic<Content.Source> sourceStatic = mockStatic(Content.Source.class);
        MockedStatic<SneakyThrows> sneaky = mockStatic(SneakyThrows.class)) {

      sourceStatic
          .when(() -> Content.Source.asInputStream(contentSource))
          .thenReturn(failingStream);
      sneaky
          .when(() -> SneakyThrows.propagate(any(IOException.class)))
          .thenReturn(new RuntimeException("Propagated exception"));

      RuntimeException thrown = assertThrows(RuntimeException.class, () -> fileUpload.bytes());
      assertEquals("Propagated exception", thrown.getMessage());
    }
  }

  @Test
  void testPath_WithPathPart() {
    // Branch 1: If it's already a PathPart, it just returns the path
    MultiPart.PathPart pathPart = mock(MultiPart.PathPart.class);
    Path existingPath = mock(Path.class);
    when(pathPart.getPath()).thenReturn(existingPath);

    JettyFileUpload pathUpload = new JettyFileUpload(tmpdir, pathPart);

    assertEquals(existingPath, pathUpload.path());
  }

  @Test
  void testPath_WithStandardPart_WritesToTempDir() throws Exception {
    // Branch 2: Standard part, creates a temp file and writes out
    Path resolvedPath = mock(Path.class);
    when(tmpdir.resolve(any(String.class))).thenReturn(resolvedPath);

    assertEquals(resolvedPath, fileUpload.path());

    verify(tmpdir).resolve(any(String.class));
    verify(upload).writeTo(resolvedPath);
  }

  @Test
  void testPath_WithStandardPart_ThrowsException() throws Exception {
    Path resolvedPath = mock(Path.class);
    when(tmpdir.resolve(any(String.class))).thenReturn(resolvedPath);

    IOException writeException = new IOException("Disk full");
    doThrow(writeException).when(upload).writeTo(resolvedPath);

    try (MockedStatic<SneakyThrows> sneaky = mockStatic(SneakyThrows.class)) {
      sneaky
          .when(() -> SneakyThrows.propagate(writeException))
          .thenReturn(new RuntimeException("Propagated disk error"));

      RuntimeException thrown = assertThrows(RuntimeException.class, () -> fileUpload.path());
      assertEquals("Propagated disk error", thrown.getMessage());
    }
  }
}
