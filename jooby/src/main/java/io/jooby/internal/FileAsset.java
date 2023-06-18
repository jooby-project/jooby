/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.MediaType;
import io.jooby.SneakyThrows;
import io.jooby.handler.Asset;

/**
 * File system asset.
 *
 * @author edgar
 * @since 2.0.0.
 */
public class FileAsset implements Asset {

  /** File. */
  private Path file;

  /**
   * Creates a new file asset.
   *
   * @param file Asset file.
   */
  public FileAsset(@NonNull Path file) {
    this.file = file;
  }

  @Override
  public long getSize() {
    try {
      return Files.size(file);
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override
  public long getLastModified() {
    try {
      return Files.getLastModifiedTime(file).toMillis();
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @NonNull @Override
  public MediaType getContentType() {
    return MediaType.byFile(file);
  }

  @Override
  public InputStream stream() {
    try {
      return new FileInputStream(file.toFile());
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override
  public void close() {
    // NOOP
  }

  @Override
  public boolean isDirectory() {
    return Files.isDirectory(file);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof io.jooby.internal.FileAsset) {
      return file.equals(((io.jooby.internal.FileAsset) obj).file);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return file.hashCode();
  }

  @Override
  public String toString() {
    return file.toString();
  }
}
