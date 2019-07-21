/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.internal.MissingValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * File upload class, file upload are available when request body is encoded as
 * {@link MediaType#MULTIPART_FORMDATA}. Example:
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
public interface FileUpload extends Value {
  /**
   * Name of file upload.
   * @return Name of file upload.
   */
  @Nonnull String getFileName();

  /**
   * Content type of file upload.
   *
   * @return Content type of file upload.
   */
  @Nullable String getContentType();

  @Override default Value get(@Nonnull int index) {
    return index == 0 ? this : get(Integer.toString(index));
  }

  @Override default Value get(@Nonnull String name) {
    return new MissingValue(name);
  }

  @Override default int size() {
    return 1;
  }

  @Nonnull @Override default Iterator<Value> iterator() {
    Iterator iterator = Collections.singletonList(this).iterator();
    return iterator;
  }

  @Override default @Nonnull String value() {
    return value(StandardCharsets.UTF_8);
  }

  /**
   * File upload content as string.
   *
   * @param charset Charset.
   * @return Content as string.
   */
  default @Nonnull String value(@Nonnull Charset charset) {
    return new String(bytes(), charset);
  }

  /**
   * Multi-value map with field name as key and file name as values.
   *
   * @return Multi-value map with field name as key and file name as values.
   */
  @Override default @Nonnull Map<String, List<String>> toMultimap() {
    Map<String, List<String>> result = new HashMap<>(1);
    result.put(name(), Collections.singletonList(getFileName()));
    return result;
  }

  /**
   * Content as input stream.
   *
   * @return Content as input stream.
   */
  @Nonnull InputStream stream();

  /**
   * Content as byte array.
   *
   * @return Content as byte array.
   */
  @Nonnull byte[] bytes();

  /**
   * File system path to access file content.
   *
   * @return File system path to access file content.
   */
  @Nonnull Path path();

  /**
   * File size.
   *
   * @return File size.
   */
  long getFileSize();

  /**
   * Free resources, delete temporary file.
   */
  void destroy();
}
