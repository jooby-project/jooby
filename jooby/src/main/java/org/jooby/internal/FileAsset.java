package org.jooby.internal;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jooby.Asset;
import org.jooby.MediaType;

class FileAsset implements Asset {

  private File file;

  private MediaType contentType;

  public FileAsset(final File file, final MediaType contentType) {
    this.file = requireNonNull(file, "A file is required.");
    this.contentType = requireNonNull(contentType, "The contentType is required.");
  }

  @Override
  public String name() {
    return file.getName();
  }

  @Override
  public InputStream stream() throws IOException {
    return new FileInputStream(file);
  }

  @Override
  public long lastModified() {
    return file.lastModified();
  }

  @Override
  public MediaType type() {
    return contentType;
  }

  @Override
  public String toString() {
    return name() + "(" + type() + ")";
  }
}
