/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.pac4j;

import org.pac4j.core.context.FrameworkParameters;

import io.jooby.Context;

/**
 * Provide access to HTTP context.
 *
 * @author edgar
 * @since 3.5.0
 */
public interface Pac4jFrameworkParameters extends FrameworkParameters {
  /**
   * HTTP context.
   *
   * @return HTTP context.
   */
  Context getContext();

  /**
   * Creates a pac4j framework parameters.
   *
   * @param ctx HTTP Context.
   * @return Framework parameters
   */
  static Pac4jFrameworkParameters create(Context ctx) {
    var custom = ctx.getRouter().getServices().getOrNull(Pac4jFrameworkParameters.class);
    return custom != null ? custom : () -> ctx;
  }
}
