/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import java.util.Collection;

import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.engine.SecurityGrantedAccessAdapter;
import org.pac4j.core.exception.http.WithLocationAction;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.core.util.Pac4jConstants;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.SneakyThrows;
import io.jooby.pac4j.Pac4jOptions;

public class GrantAccessAdapterImpl implements SecurityGrantedAccessAdapter {
  private final Context ctx;
  private final Pac4jOptions config;
  private final SneakyThrows.Function2<WebContext, SessionStore, Object> next;

  public GrantAccessAdapterImpl(Context ctx, Pac4jOptions config) {
    this(
        ctx,
        config,
        (context, sessionStore) -> {
          var requestedUrl =
              sessionStore
                  .get(context, Pac4jConstants.REQUESTED_URL)
                  .filter(WithLocationAction.class::isInstance)
                  .map(it -> ((WithLocationAction) it).getLocation())
                  .orElse(config.getDefaultUrl());
          return ctx.sendRedirect(requestedUrl);
        });
  }

  public GrantAccessAdapterImpl(Context ctx, Pac4jOptions config, Route.Handler next) {
    this(ctx, config, (context, sessionStore) -> next.apply(ctx));
  }

  private GrantAccessAdapterImpl(
      Context ctx,
      Pac4jOptions config,
      SneakyThrows.Function2<WebContext, SessionStore, Object> next) {
    this.ctx = ctx;
    this.config = config;
    this.next = next;
  }

  @Override
  public Object adapt(
      WebContext context, SessionStore sessionStore, Collection<UserProfile> profiles)
      throws Exception {
    var iterator = profiles.iterator();
    if (iterator.hasNext()) {
      ctx.setUser(iterator.next());
    }
    return next.apply(context, sessionStore);
  }
}
