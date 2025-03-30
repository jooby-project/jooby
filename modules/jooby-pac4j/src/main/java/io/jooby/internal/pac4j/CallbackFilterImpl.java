/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Route;
import io.jooby.SneakyThrows;
import io.jooby.pac4j.Pac4jFrameworkParameters;
import io.jooby.pac4j.Pac4jOptions;

public class CallbackFilterImpl implements Route.Handler {

  private final Pac4jOptions config;

  public CallbackFilterImpl(Pac4jOptions config) {
    this.config = config;
  }

  @NonNull @Override
  public Object apply(@NonNull Context ctx) throws Exception {
    try {
      var result =
          config
              .getCallbackLogic()
              .perform(
                  config,
                  config.getDefaultUrl(),
                  config.getRenewSession(),
                  config.getDefaultClient(),
                  Pac4jFrameworkParameters.create(ctx));

      return result == null ? ctx : result;
    } catch (RuntimeException re) {
      if (re.getCause() != null) {
        throw SneakyThrows.propagate(re.getCause());
      }
      throw re;
    }
  }
}
