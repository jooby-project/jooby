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

public class CallbackFilterImpl implements Route.Handler {

  private final Pac4jOptions options;

  private Config config;

  public CallbackFilterImpl(Config config, Pac4jOptions options) {
    this.config = config;
    this.options = options;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) throws Exception {
    Pac4jContext pac4j = Pac4jContext.create(ctx, options);

    Object result = config.getCallbackLogic().perform(
        pac4j,
        config,
        config.getHttpActionAdapter(),
        options.getDefaultUrl(),
        options.getSaveInSession(),
        options.getMultiProfile(),
        options.getRenewSession(),
        options.getDefaultClient()
    );

    return result == null ? ctx : result;
  }

}
