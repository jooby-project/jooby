/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import org.pac4j.core.context.FrameworkParameters;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.context.session.SessionStoreFactory;

import io.jooby.pac4j.Pac4jContext;

public class SessionStoreFactoryImpl implements SessionStoreFactory {
  @Override
  public SessionStore newSessionStore(FrameworkParameters parameters) {
    if (parameters instanceof Pac4jContext ctx) {
      return ctx.getSessionStore();
    }
    return new SessionStoreImpl();
  }
}
