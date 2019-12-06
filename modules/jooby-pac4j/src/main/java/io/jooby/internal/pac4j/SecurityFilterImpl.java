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
import org.pac4j.core.client.Client;
import org.pac4j.core.client.finder.ClientFinder;
import org.pac4j.core.client.finder.DefaultSecurityClientFinder;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.engine.DefaultSecurityLogic;
import org.pac4j.core.engine.SecurityLogic;
import org.pac4j.core.exception.http.WithLocationAction;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

public class SecurityFilterImpl implements Route.Decorator, Route.Handler {

  private String pattern;

  private Config config;

  private Pac4jOptions options;

  private String clients;

  private String authorizers;

  public SecurityFilterImpl(String pattern, Config config, Pac4jOptions options, List<Client> clients) {
    this.pattern = pattern;
    this.config = config;
    this.options = options;
    this.clients = clients.stream().map(it -> it.getName())
        .collect(Collectors.joining(Pac4jConstants.ELEMENT_SEPARATOR));
  }

  public SecurityFilterImpl addAuthorizer(String authorizer) {
    if (authorizers == null) {
      authorizers = authorizer;
    } else {
      authorizers += Pac4jConstants.ELEMENT_SEPARATOR + authorizer;
    }
    return this;
  }

  @Nonnull @Override public Route.Handler apply(@Nonnull Route.Handler next) {
    return ctx -> {
      if (pattern == null) {
        return perform(Pac4jContext.create(ctx), new GrantAccessAdapterImpl(ctx, next));
      } else {
        if (ctx.matches(pattern)) {
          return perform(Pac4jContext.create(ctx), new GrantAccessAdapterImpl(ctx, next));
        } else {
          return next.apply(ctx);
        }
      }
    };
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) throws Exception {
    Pac4jContext pac4j = Pac4jContext.create(ctx);
    String requestedUrl = (String) pac4j.getSessionStore().get(pac4j, Pac4jConstants.REQUESTED_URL)
        .filter(WithLocationAction.class::isInstance)
        .map(it -> ((WithLocationAction) it).getLocation())
        .orElse(options.getDefaultUrl());
    return perform(pac4j, GrantAccessAdapterImpl.redirect(ctx, requestedUrl));
  }

  private Object perform(Pac4jContext ctx, GrantAccessAdapterImpl grantAccessAdapter) {
    SecurityLogic securityLogic = config.getSecurityLogic();
    String clients = ctx.getContext().query(clientName(securityLogic))
        .value(this.clients);
    return securityLogic.perform(ctx, config, grantAccessAdapter, config.getHttpActionAdapter(),
        clients, authorizers, null, options.getMultiProfile());
  }

  private String clientName(SecurityLogic securityLogic) {
    if (securityLogic instanceof DefaultSecurityLogic) {
      ClientFinder finder = ((DefaultSecurityLogic) securityLogic).getClientFinder();
      if (finder instanceof DefaultSecurityClientFinder) {
        return ((DefaultSecurityClientFinder) finder).getClientNameParameter();
      }
    }
    return Pac4jConstants.DEFAULT_CLIENT_NAME_PARAMETER;
  }

}
