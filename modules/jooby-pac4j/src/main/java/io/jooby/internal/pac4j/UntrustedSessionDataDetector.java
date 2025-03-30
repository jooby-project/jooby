/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Route;
import io.jooby.Session;

public class UntrustedSessionDataDetector implements Route.Filter {
  @Override
  @NonNull public Route.Handler apply(@NonNull Route.Handler next) {
    return ctx -> {
      Session session = ctx.sessionOrNull();
      if (session instanceof Pac4jSession) {
        return session;
      }
      return session == null ? next.apply(ctx) : next.apply(Pac4jSession.create(ctx));
    };
  }
}
