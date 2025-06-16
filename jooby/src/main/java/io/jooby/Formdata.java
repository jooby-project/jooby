/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.util.Collection;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.internal.MultipartNode;
import io.jooby.value.ValueFactory;

/**
 * Form class for direct MVC parameter provisioning.
 *
 * <p>HTTP request must be encoded as {@link MediaType#FORM_URLENCODED} or {@link
 * MediaType#MULTIPART_FORMDATA}.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface Formdata extends ValueNode {

  /**
   * Add a form field.
   *
   * @param path Form name/path.
   * @param value Form value.
   */
  @NonNull
  void put(@NonNull String path, @NonNull ValueNode value);

  /**
   * Add a form field.
   *
   * @param path Form name/path.
   * @param value Form value.
   */
  @NonNull
  void put(@NonNull String path, @NonNull String value);

  /**
   * Add a form field.
   *
   * @param path Form name/path.
   * @param values Form values.
   */
  @NonNull
  void put(@NonNull String path, @NonNull Collection<String> values);

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
   * <p>Only for <code>multipart/form-data</code> request.
   *
   * @param name Field name. Please note this is the form field name, not the actual file name.
   * @return All file uploads.
   */
  @NonNull List<FileUpload> files(@NonNull String name);

  /**
   * A file upload that matches the given field name.
   *
   * <p>Only for <code>multipart/form-data</code> request.
   *
   * @param name Field name. Please note this is the form field name, not the actual file name.
   * @return A file upload.
   */
  @NonNull FileUpload file(@NonNull String name);

  /**
   * Creates a new multipart object.
   *
   * @param valueFactory Current context.
   * @return Multipart instance.
   */
  static @NonNull Formdata create(@NonNull ValueFactory valueFactory) {
    return new MultipartNode(valueFactory);
  }
}
