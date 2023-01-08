/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.io.InputStream;
import java.nio.file.Path;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * File upload class, file upload are available when request body is encoded as {@link
 * MediaType#MULTIPART_FORMDATA}. Example:
 *
 * <pre>{@code
 * {
 *
 *   post("/submit", ctx -> {
 *
 *     FileUpload file = ctx.file("myfile");
 *
 *   });
 *
 * }
 * }</pre>
 *
 * @since 2.0.0
 * @author edgar
 */
public interface FileUpload extends java.io.Closeable {

  /**
   * File key. That's the field form name, not the file name. Example:
   *
   * @return File key. That's the field form name, not the file name.
   */
  @NonNull String getName();

  /**
   * Name of file upload.
   *
   * @return Name of file upload.
   */
  @NonNull String getFileName();

  /**
   * Content type of file upload.
   *
   * @return Content type of file upload.
   */
  @Nullable String getContentType();

  /**
   * Content as input stream.
   *
   * @return Content as input stream.
   */
  @NonNull InputStream stream();

  /**
   * Content as byte array.
   *
   * @return Content as byte array.
   */
  @NonNull byte[] bytes();

  /**
   * File system path to access file content.
   *
   * @return File system path to access file content.
   */
  @NonNull Path path();

  /**
   * File size or <code>-1</code> when unknown.
   *
   * @return File size or <code>-1</code> when unknown.
   */
  long getFileSize();

  /** Free resources, delete temporary file. */
  @Override
  void close();
}
