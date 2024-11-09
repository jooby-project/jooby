/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import java.util.function.Function;

import org.pac4j.core.config.Config;

import io.jooby.Context;
import io.jooby.pac4j.Pac4jContext;

public class Pac4jCurrentUser implements Function<Context, Object> {

  private final Config config;

  public Pac4jCurrentUser(Config config) {
    this.config = config;
  }

  @Override
  public Object apply(Context ctx) {
    var pmf = config.getProfileManagerFactory();
    var pac4jContext = Pac4jContext.create(ctx);
    var pm = pmf.apply(pac4jContext, pac4jContext.getSessionStore());
    return pm.getProfile().orElse(null);
  }
}
