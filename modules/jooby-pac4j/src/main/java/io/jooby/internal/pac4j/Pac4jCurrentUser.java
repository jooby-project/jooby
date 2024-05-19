/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import java.util.function.Function;

import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.config.Config;

import io.jooby.Context;
import io.jooby.pac4j.Pac4jContext;

public class Pac4jCurrentUser implements Function<Context, Object> {

  @Override
  public Object apply(Context ctx) {
    Pac4jContext pac4jContext = Pac4jContext.create(ctx);
    ProfileManager pm = new ProfileManager(pac4jContext, pac4jContext.getSessionStore());
    pm.setConfig(ctx.require(Config.class));
    return pm.getProfile().orElse(null);
  }
}
