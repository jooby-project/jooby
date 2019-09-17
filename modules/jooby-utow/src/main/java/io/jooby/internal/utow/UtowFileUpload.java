/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.utow;

import io.jooby.FileUpload;
import io.jooby.ServerOptions;
import io.jooby.SneakyThrows;
import io.undertow.server.handlers.form.FormData;
import io.undertow.util.Headers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public class UtowFileUpload implements FileUpload {

  private final FormData.FormValue upload;

  public UtowFileUpload(FormData.FormValue upload) {
    this.upload = upload;
  }

  @Override public byte[] bytes() {
    try (InputStream in = stream()) {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream(ServerOptions._16KB);
      byte[] data = new byte[ServerOptions._16KB];
      int nRead;
      while ((nRead = in.read(data, 0, data.length)) != -1) {
        buffer.write(data, 0, nRead);
      }
      return buffer.toByteArray();
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override public InputStream stream() {
    try {
      return upload.getFileItem().getInputStream();
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override public String getFileName() {
    return upload.getFileName();
  }

  @Override public String getContentType() {
    return upload.getHeaders().getFirst(Headers.CONTENT_TYPE);
  }

  @Override public Path path() {
    return upload.getFileItem().getFile();
  }

  @Override public long getFileSize() {
    return Long.parseLong(upload.getHeaders().getFirst(Headers.CONTENT_LENGTH));
  }

  @Override public void destroy() {
    try {
      upload.getFileItem().delete();
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override public String toString() {
    return getFileName();
  }
}
