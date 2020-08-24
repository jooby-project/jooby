/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Represents an inline file response.
 *
 * @author edgar
 * @since 2.0.0
 */
public class InlineFile extends FileDownload {

  /**
   * Creates a new inline file.
   *
   * @param content File content.
   * @param fileName Filename.
   * @param fileSize File size or <code>-1</code> if unknown.
   */
  public InlineFile(@Nonnull InputStream content, @Nonnull String fileName, long fileSize) {
    super(Mode.INLINE, content, fileName, fileSize);
  }

  /**
   * Creates a new inline file.
   *
   * @param content File content.
   * @param fileName Filename.
   */
  public InlineFile(@Nonnull InputStream content, @Nonnull String fileName) {
    super(Mode.INLINE, content, fileName);
  }

  /**
   * Creates a new inline file.
   *
   * @param file File content.
   * @param fileName Filename.
   * @throws IOException For IO exception while reading file.
   */
  public InlineFile(@Nonnull Path file, @Nonnull String fileName) throws IOException {
    super(Mode.INLINE, file, fileName);
  }

  /**
   * Creates a new inline file.
   *
   * @param file File content.
   * @throws IOException For IO exception while reading file.
   */
  public InlineFile(@Nonnull Path file) throws IOException {
    super(Mode.INLINE, file);
  }
}
