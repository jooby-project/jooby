/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import org.pac4j.core.config.Config;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Route;
import io.jooby.pac4j.Pac4jContext;
import io.jooby.pac4j.Pac4jOptions;

public class LogoutImpl implements Route.Handler {

  private final Config config;

  private final Pac4jOptions options;

  public LogoutImpl(Config config, Pac4jOptions options) {
    this.config = config;
    this.options = options;
  }

  @NonNull @Override
  public Object apply(@NonNull Context ctx) throws Exception {
    String redirectTo = (String) ctx.getAttributes().get("pac4j.logout.redirectTo");
    if (redirectTo == null || redirectTo.length() == 0) {
      redirectTo = options.getDefaultUrl();
    }
    redirectTo = ctx.getRequestURL(redirectTo);
    Pac4jContext pac4jContext = Pac4jContext.create(ctx);
    return config
        .getLogoutLogic()
        .perform(
            pac4jContext,
            pac4jContext.getSessionStore(),
            config,
            config.getHttpActionAdapter(),
            redirectTo,
            null,
            options.isLocalLogout(),
            options.isDestroySession(),
            options.isCentralLogout());
  }
}
