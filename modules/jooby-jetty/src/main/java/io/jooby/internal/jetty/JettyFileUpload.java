/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.MultiPart;
import org.eclipse.jetty.io.Content;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.FileUpload;
import io.jooby.SneakyThrows;

public class JettyFileUpload implements FileUpload {
  private final MultiPart.Part upload;
  private final Path tmpdir;

  public JettyFileUpload(Path tmpdir, MultiPart.Part upload) {
    this.tmpdir = tmpdir;
    this.upload = upload;
  }

  @NonNull @Override
  public String getName() {
    return upload.getName();
  }

  @Override
  public @NonNull String getFileName() {
    return upload.getFileName();
  }

  @Override
  public @NonNull byte[] bytes() {
    try (var in = stream()) {
      return in.readAllBytes();
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override
  public @NonNull InputStream stream() {
    try {
      return Content.Source.asInputStream(upload.getContentSource());
    } catch (Exception c) {
      return null;
    }
  }

  @Override
  public String getContentType() {
    return upload.getHeaders().get(HttpHeader.CONTENT_TYPE);
  }

  @Override
  public @NonNull Path path() {
    try {
      if (upload instanceof MultiPart.PathPart pathPart) {
        return pathPart.getPath();
      }
      var path = tmpdir.resolve("jetty" + System.currentTimeMillis() + ".tmp");
      upload.writeTo(path);
      return path;
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override
  public long getFileSize() {
    return upload.getLength();
  }

  @Override
  public void close() {
    upload.close();
  }

  @Override
  public String toString() {
    return getFileName();
  }
}
