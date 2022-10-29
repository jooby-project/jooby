/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import java.util.Set;

import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.engine.savedrequest.DefaultSavedRequestHandler;

import io.jooby.Context;
import io.jooby.pac4j.Pac4jContext;

public class SavedRequestHandlerImpl extends DefaultSavedRequestHandler {
  private Set<String> excludes;

  public SavedRequestHandlerImpl(Set<String> excludes) {
    this.excludes = excludes;
  }

  @Override
  public void save(WebContext webContext, SessionStore sessionStore) {
    Pac4jContext pac4j = (Pac4jContext) webContext;
    Context context = pac4j.getContext();
    if (!excludes.contains(context.getRequestPath())) {
      super.save(webContext, sessionStore);
    }
  }
}
