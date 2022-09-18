/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.internal.MultipartNode;

import edu.umd.cs.findbugs.annotations.NonNull;
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
  void put(@NonNull String name, @NonNull FileUpload file);

  /**
   * All file uploads. Only for <code>multipart/form-data</code> request.
   *
   * @return All file uploads.
   */
  @NonNull List<FileUpload> files();

  /**
   * All file uploads that matches the given field name.
   *
   * Only for <code>multipart/form-data</code> request.
   *
   * @param name Field name. Please note this is the form field name, not the actual file name.
   * @return All file uploads.
   */
  @NonNull List<FileUpload> files(@NonNull String name);

  /**
   * A file upload that matches the given field name.
   *
   * Only for <code>multipart/form-data</code> request.
   *
   * @param name Field name. Please note this is the form field name, not the actual file name.
   * @return A file upload.
   */
  @NonNull FileUpload file(@NonNull String name);

  /**
   * Creates a new multipart object.
   *
   * @param ctx Current context.
   * @return Multipart instance.
   */
  static @NonNull Multipart create(@NonNull Context ctx) {
    return new MultipartNode(ctx);
  }
}
