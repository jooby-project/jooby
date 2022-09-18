/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.internal.FormdataNode;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Collection;

/**
 * Formdata class for direct MVC parameter provisioning.
 *
 * HTTP request must be encoded as {@link MediaType#FORM_URLENCODED}.
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
  @NonNull void put(@NonNull String path, @NonNull ValueNode value);

  /**
   * Add a form field.
   *
   * @param path Form name/path.
   * @param value Form value.
   */
  @NonNull void put(@NonNull String path, @NonNull String value);

  /**
   * Add a form field.
   *
   * @param path Form name/path.
   * @param values Form values.
   */
  @NonNull void put(@NonNull String path, @NonNull Collection<String> values);

  /**
   * Creates a formdata object.
   *
   * @param ctx Current context.
   * @return Formdata.
   */
  static @NonNull Formdata create(@NonNull Context ctx) {
    return new FormdataNode(ctx);
  }
}
