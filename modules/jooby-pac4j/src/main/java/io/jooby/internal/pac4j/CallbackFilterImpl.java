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

public class CallbackFilterImpl implements Route.Handler {

  private final Pac4jOptions options;

  private final Config config;

  public CallbackFilterImpl(Config config, Pac4jOptions options) {
    this.config = config;
    this.options = options;
  }

  @NonNull @Override
  public Object apply(@NonNull Context ctx) throws Exception {
    Pac4jContext pac4j = Pac4jContext.create(ctx);

    Object result =
        config
            .getCallbackLogic()
            .perform(
                pac4j,
                config,
                config.getHttpActionAdapter(),
                options.getDefaultUrl(),
                options.getSaveInSession(),
                options.getMultiProfile(),
                options.getRenewSession(),
                options.getDefaultClient());

    return result == null ? ctx : result;
  }
}
