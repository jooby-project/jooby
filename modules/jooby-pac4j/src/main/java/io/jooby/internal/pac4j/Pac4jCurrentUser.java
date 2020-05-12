/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import io.jooby.Context;
import io.jooby.pac4j.Pac4jContext;
import org.pac4j.core.profile.ProfileManager;

import java.util.function.Function;

public class Pac4jCurrentUser implements Function<Context, Object> {
  @Override public Object apply(Context ctx) {
    ProfileManager pm = new ProfileManager(Pac4jContext.create(ctx));
    return pm.get(ctx.sessionOrNull() != null).orElse(null);
  }
}
