/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.utow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.jooby.internal.undertow.UndertowFileUpload;
import io.undertow.server.handlers.form.FormData;

public class UndertowFileUploadTest {

  @Test
  public void shouldGetFileSize() throws IOException {
    long fileSize = 678;

    FormData.FileItem fileItem = mock(FormData.FileItem.class);
    when(fileItem.getFileSize()).thenReturn(fileSize);

    FormData.FormValue upload = mock(FormData.FormValue.class);
    when(upload.getFileItem()).thenReturn(fileItem);

    assertEquals(fileSize, new UndertowFileUpload("file", upload).getFileSize());
  }

  @Test
  public void shouldGetUnknownFileSize() throws IOException {
    long fileSize = -1;

    FormData.FileItem fileItem = mock(FormData.FileItem.class);
    doThrow(new IOException()).when(fileItem).getFileSize();

    FormData.FormValue upload = mock(FormData.FormValue.class);
    when(upload.getFileItem()).thenReturn(fileItem);

    assertEquals(fileSize, new UndertowFileUpload("file", upload).getFileSize());
  }
}
