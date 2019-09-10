/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.internal.HashValue;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * Formdata class for direct MVC parameter provisioning.
 *
 * HTTP request must be encoded as {@link MediaType#FORM_URLENCODED}.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface Formdata extends Value {

  /**
   * Add a form field.
   *
   * @param path Form name/path.
   * @param value Form value.
   * @return This formdata.
   */
  @Nonnull Formdata put(@Nonnull String path, @Nonnull Value value);

  /**
   * Add a form field.
   *
   * @param path Form name/path.
   * @param value Form value.
   * @return This formdata.
   */
  @Nonnull Formdata put(@Nonnull String path, @Nonnull String value);

  /**
   * Add a form field.
   *
   * @param path Form name/path.
   * @param values Form values.
   * @return This formdata.
   */
  @Nonnull Formdata put(@Nonnull String path, @Nonnull Collection<String> values);

  /**
   * Creates a formdata object.
   *
   * @return Formdata.
   */
  static @Nonnull Formdata create() {
    return new HashValue(null);
  }
}
