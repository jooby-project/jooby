/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.pac4j.Pac4jContext;
import io.jooby.pac4j.Pac4jOptions;
import org.pac4j.core.config.Config;

import javax.annotation.Nonnull;

public class LogoutImpl implements Route.Handler {

  private final Config config;

  private final Pac4jOptions options;

  public LogoutImpl(Config config, Pac4jOptions options) {
    this.config = config;
    this.options = options;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) throws Exception {
    String redirectTo = (String) ctx.getAttributes().get("pac4j.logout.redirectTo");
    if (redirectTo == null || redirectTo.length() == 0) {
      redirectTo = options.getDefaultUrl();
    }
    redirectTo = ctx.getRequestURL(redirectTo);
    return config.getLogoutLogic()
        .perform(Pac4jContext.create(ctx, options), config, config.getHttpActionAdapter(), redirectTo,
            null, options.isLocalLogout(), options.isDestroySession(), options.isCentralLogout());
  }
}
