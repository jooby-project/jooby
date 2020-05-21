/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.internal.MultipartNode;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Multipart class for direct MVC parameter provisioning.
 *
 * HTTP request must be encoded as {@link MediaType#MULTIPART_FORMDATA}.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface Multipart extends Formdata {

  /**
   * Put/Add a file into this multipart request.
   *
   * @param name HTTP name.
   * @param file File upload.
   */
  void put(@Nonnull String name, @Nonnull FileUpload file);

  /**
   * All file uploads. Only for <code>multipart/form-data</code> request.
   *
   * @return All file uploads.
   */
  @Nonnull List<FileUpload> files();

  /**
   * All file uploads that matches the given field name.
   *
   * Only for <code>multipart/form-data</code> request.
   *
   * @param name Field name. Please note this is the form field name, not the actual file name.
   * @return All file uploads.
   */
  @Nonnull List<FileUpload> files(@Nonnull String name);

  /**
   * A file upload that matches the given field name.
   *
   * Only for <code>multipart/form-data</code> request.
   *
   * @param name Field name. Please note this is the form field name, not the actual file name.
   * @return A file upload.
   */
  @Nonnull FileUpload file(@Nonnull String name);

  /**
   * Creates a new multipart object.
   *
   * @param ctx Current context.
   * @return Multipart instance.
   */
  static @Nonnull Multipart create(@Nonnull Context ctx) {
    return new MultipartNode(ctx);
  }
}
