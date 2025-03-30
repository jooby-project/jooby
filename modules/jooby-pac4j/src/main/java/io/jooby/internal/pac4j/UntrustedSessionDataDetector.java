package io.jooby.internal.pac4j;

import io.jooby.Route;
import io.jooby.Session;

import javax.annotation.Nonnull;

public class UntrustedSessionDataDetector implements Route.Decorator {
  @Nonnull
  @Override
  public Route.Handler apply(@Nonnull Route.Handler next) {
    return ctx -> {
      Session session = ctx.sessionOrNull();
      if (session instanceof Pac4jSession) {
        return session;
      }
      return session == null ? next.apply(ctx) : next.apply(Pac4jSession.create(ctx));
    };
  }
}
