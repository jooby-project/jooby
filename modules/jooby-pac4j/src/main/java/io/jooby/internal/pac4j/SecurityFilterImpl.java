/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import static java.util.Optional.ofNullable;

import java.util.List;
import java.util.function.Supplier;

import org.pac4j.core.adapter.FrameworkAdapter;
import org.pac4j.core.client.finder.ClientFinder;
import org.pac4j.core.client.finder.DefaultSecurityClientFinder;
import org.pac4j.core.config.Config;
import org.pac4j.core.engine.DefaultSecurityLogic;
import org.pac4j.core.engine.SecurityLogic;
import org.pac4j.core.util.Pac4jConstants;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Route;
import io.jooby.pac4j.Pac4jFrameworkParameters;
import io.jooby.pac4j.Pac4jOptions;

public class SecurityFilterImpl implements Route.Filter, Route.Handler {

  private final String pattern;

  private final Config config;

  private final Pac4jOptions options;

  private final Supplier<String> clients;

  private String authorizers;

  public SecurityFilterImpl(
      String pattern,
      Config config,
      Pac4jOptions options,
      Supplier<String> clients,
      List<String> authorizers) {
    this.pattern = pattern;
    this.config = config;
    this.options = options;
    this.clients = clients;
    authorizers.forEach(this::addAuthorizer);
  }

  public void addAuthorizer(String authorizer) {
    if (authorizers == null) {
      authorizers = authorizer;
    } else {
      authorizers += Pac4jConstants.ELEMENT_SEPARATOR + authorizer;
    }
  }

  @NonNull @Override
  public Route.Handler apply(@NonNull Route.Handler next) {
    return ctx -> {
      if (pattern == null) {
        return perform(ctx, new GrantAccessAdapterImpl(ctx, options, next));
      } else {
        if (ctx.matches(pattern)) {
          return perform(ctx, new GrantAccessAdapterImpl(ctx, options, next));
        } else {
          return next.apply(ctx);
        }
      }
    };
  }

  @NonNull @Override
  public Object apply(@NonNull Context ctx) throws Exception {
    return perform(ctx, new GrantAccessAdapterImpl(ctx, options));
  }

  private Object perform(Context ctx, GrantAccessAdapterImpl accessAdapter) throws Exception {
    FrameworkAdapter.INSTANCE.applyDefaultSettingsIfUndefined(config);
    var securityLogic = config.getSecurityLogic();
    var clients = ctx.lookup(clientName(securityLogic)).value(this.clients.get());
    var authorizers = ofNullable(this.authorizers).orElse(NoopAuthorizer.NAME);
    return securityLogic.perform(
        config, accessAdapter, clients, authorizers, null, Pac4jFrameworkParameters.create(ctx));
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
