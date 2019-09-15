/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.internal.HashValue;

import javax.annotation.Nonnull;

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
   * Creates a new multipart object.
   *
   * @param ctx Current context.
   * @return Multipart instance.
   */
  static @Nonnull Multipart create(@Nonnull Context ctx) {
    return new HashValue(ctx, null);
  }
}
