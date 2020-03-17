/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.pac4j;

import io.jooby.Context;
import io.jooby.internal.pac4j.WebContextImpl;
import org.pac4j.core.context.WebContext;

import javax.annotation.Nonnull;

/**
 * Pac4j web context.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface Pac4jContext extends WebContext {
  /**
   * Get underlying context.
   *
   * @return The underlying context.
   */
  @Nonnull Context getContext();

  /**
   * Wrap a Web context as pac4j context.
   *
   * @param ctx Web context.
   * @return Pac4j web context.
   */
  static @Nonnull Pac4jContext create(@Nonnull Context ctx, @Nonnull Pac4jOptions options) {
    return new WebContextImpl(ctx, options);
  }
}
