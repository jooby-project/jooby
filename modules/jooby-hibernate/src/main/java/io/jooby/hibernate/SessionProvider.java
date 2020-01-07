/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.hibernate;

import org.hibernate.Session;
import org.hibernate.SessionBuilder;

import javax.annotation.Nonnull;

/**
 * Allow to customize a Session before opening it.
 *
 * @author edgar
 * @since 2.5.1
 */
public interface SessionProvider {
  /**
   * Creates a new session.
   *
   * @param builder Session builder.
   * @return A new session.
   */
  @Nonnull Session newSession(@Nonnull SessionBuilder builder);
}
