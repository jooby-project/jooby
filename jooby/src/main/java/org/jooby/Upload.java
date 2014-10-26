package org.jooby;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import javax.annotation.Nonnull;

/**
 * File upload from a browser on {@link MediaType#multipart} request.
 *
 * @author edgar
 * @since 0.1.0
 */
public interface Upload extends Closeable {

  /**
   * @return File's name.
   */
  @Nonnull
  String name();

  /**
   * @return File media type.
   */
  @Nonnull
  MediaType type();

  /**
   * Upload header, like content-type, charset, etc...
   *
   * @param name Header's name.
   * @return A header value.
   */
  @Nonnull
  Variant header(@Nonnull String name);

  /**
   * Get this upload as temporary file.
   *
   * @return A temp file.
   * @throws IOException If file doesn't exist.
   */
  @Nonnull File file() throws IOException;

}
