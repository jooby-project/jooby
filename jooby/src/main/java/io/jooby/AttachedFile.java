/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Represents a file attachment response.
 *
 * @author edgar
 * @since 2.0.0
 */
public class AttachedFile extends FileDownload {

  /**
   * Creates a new file attachment.
   *
   * @param content File content.
   * @param fileName Filename.
   * @param fileSize File size or <code>-1</code> if unknown.
   */
  public AttachedFile(@NonNull InputStream content, @NonNull String fileName, long fileSize) {
    super(Mode.ATTACHMENT, content, fileName, fileSize);
  }

  /**
   * Creates a new file attachment.
   *
   * @param content File content.
   * @param fileName Filename.
   */
  public AttachedFile(@NonNull InputStream content, @NonNull String fileName) {
    super(Mode.ATTACHMENT, content, fileName);
  }

  /**
   * Creates a new file attachment.
   *
   * @param file File content.
   * @param fileName Filename.
   * @throws IOException For IO exception while reading file.
   */
  public AttachedFile(@NonNull Path file, @NonNull String fileName) throws IOException {
    super(Mode.ATTACHMENT, file, fileName);
  }

  /**
   * Creates a new file attachment.
   *
   * @param file File content.
   * @throws IOException For IO exception while reading file.
   */
  public AttachedFile(@NonNull Path file) throws IOException {
    super(Mode.ATTACHMENT, file);
  }
}
