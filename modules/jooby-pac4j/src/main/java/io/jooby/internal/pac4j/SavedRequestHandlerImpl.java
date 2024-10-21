/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import java.util.Set;

import org.pac4j.core.context.CallContext;
import org.pac4j.core.engine.savedrequest.DefaultSavedRequestHandler;

import io.jooby.pac4j.Pac4jContext;

public class SavedRequestHandlerImpl extends DefaultSavedRequestHandler {
  private final Set<String> excludes;

  public SavedRequestHandlerImpl(Set<String> excludes) {
    this.excludes = excludes;
  }

  @Override
  public void save(CallContext ctx) {
    var pac4j = (Pac4jContext) ctx.webContext();
    var context = pac4j.getContext();
    if (!excludes.contains(context.getRequestPath())) {
      super.save(ctx);
    }
  }
}
