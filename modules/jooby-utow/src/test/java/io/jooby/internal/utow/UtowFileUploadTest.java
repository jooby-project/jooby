package io.jooby.internal.utow;

import io.undertow.server.handlers.form.FormData;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UtowFileUploadTest {

  @Test
  public void shouldGetFileSize() throws IOException {
    long fileSize = 678;

    FormData.FileItem fileItem = mock(FormData.FileItem.class);
    when(fileItem.getFileSize()).thenReturn(fileSize);

    FormData.FormValue upload = mock(FormData.FormValue.class);
    when(upload.getFileItem()).thenReturn(fileItem);

    assertEquals(fileSize, new UtowFileUpload(upload).getFileSize());
  }

  @Test
  public void shouldGetUnknownFileSize() throws IOException {
    long fileSize = -1;

    FormData.FileItem fileItem = mock(FormData.FileItem.class);
    doThrow(new IOException()).when(fileItem).getFileSize();

    FormData.FormValue upload = mock(FormData.FormValue.class);
    when(upload.getFileItem()).thenReturn(fileItem);

    assertEquals(fileSize, new UtowFileUpload(upload).getFileSize());
  }
}
