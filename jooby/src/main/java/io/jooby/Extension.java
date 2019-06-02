/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;

/**
 * Simple extension contract for adding and reusing commons application infrastructure components
 * and/or integrate with external libraries.
 *
 * Extensions are expected to work via side-effects.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface Extension {

  default boolean lateinit() {
    return false;
  }

  /**
   * Install, configure additional features to a Jooby application.
   *
   * @param application Jooby application.
   * @throws Exception If something goes wrong.
   */
  void install(@Nonnull Jooby application) throws Exception;
}
