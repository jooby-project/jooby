/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import java.util.Collection;
import java.util.Iterator;

import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.engine.SecurityGrantedAccessAdapter;
import org.pac4j.core.profile.UserProfile;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.pac4j.Pac4jContext;

public class GrantAccessAdapterImpl implements SecurityGrantedAccessAdapter {
  private final Context ctx;
  private final Route.Handler next;

  public GrantAccessAdapterImpl(Context ctx, Route.Handler next) {
    this.ctx = ctx;
    this.next = next;
  }

  @Override
  public Object adapt(
      WebContext webContext,
      SessionStore sessionStore,
      Collection<UserProfile> profiles,
      Object... objects)
      throws Exception {
    Iterator<UserProfile> iterator = profiles.iterator();
    if (iterator.hasNext()) {
      ((Pac4jContext) webContext).getContext().setUser(iterator.next());
    }
    return next.apply(ctx);
  }

  public static GrantAccessAdapterImpl redirect(Context context, String redirectTo) {
    return new GrantAccessAdapterImpl(context, ctx -> ctx.sendRedirect(redirectTo));
  }
}
