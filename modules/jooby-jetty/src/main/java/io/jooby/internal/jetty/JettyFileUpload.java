/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import io.jooby.FileUpload;
import io.jooby.SneakyThrows;
import org.eclipse.jetty.http.MultiPartFormInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class JettyFileUpload implements FileUpload {
  private final MultiPartFormInputStream.MultiPart upload;

  public JettyFileUpload(MultiPartFormInputStream.MultiPart upload) {
    this.upload = upload;
  }

  @Override public String getFileName() {
    return upload.getSubmittedFileName();
  }

  @Override public byte[] bytes() {
    try {
      byte[] bytes = upload.getBytes();
      if (bytes == null) {
        return Files.readAllBytes(upload.getFile().toPath());
      }
      return bytes;
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override public InputStream stream() {
    try {
      return upload.getInputStream();
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override public String getContentType() {
    return upload.getContentType();
  }

  @Override public Path path() {
    try {
      if (upload.getFile() == null) {
        upload.write("jetty" + System.currentTimeMillis() + ".tmp");
      }
      return upload.getFile().toPath();
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override public long getFileSize() {
    return upload.getSize();
  }

  @Override public void destroy() {
    try {
      upload.cleanUp();
      upload.delete();
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override public String toString() {
    return getFileName();
  }
}
