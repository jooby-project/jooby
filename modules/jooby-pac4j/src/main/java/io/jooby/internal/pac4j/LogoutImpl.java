/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import org.pac4j.core.adapter.FrameworkAdapter;
import org.pac4j.core.config.Config;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Route;
import io.jooby.pac4j.Pac4jFrameworkParameters;
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
    FrameworkAdapter.INSTANCE.applyDefaultSettingsIfUndefined(config);
    var redirectTo = (String) ctx.getAttributes().get("pac4j.logout.redirectTo");
    if (redirectTo == null || redirectTo.isEmpty()) {
      redirectTo = options.getDefaultUrl();
    }
    redirectTo = ctx.getRequestURL(redirectTo);
    return config
        .getLogoutLogic()
        .perform(
            config,
            redirectTo,
            options.getLogoutUrlPattern(),
            options.isLocalLogout(),
            options.isDestroySession(),
            options.isCentralLogout(),
            Pac4jFrameworkParameters.create(ctx));
  }
}
