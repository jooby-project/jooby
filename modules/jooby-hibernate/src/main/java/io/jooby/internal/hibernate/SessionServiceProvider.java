/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.hibernate;

import io.jooby.hibernate.SessionProvider;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.context.internal.ManagedSessionContext;

import javax.inject.Provider;

public class SessionServiceProvider implements Provider<Session> {
  private final SessionFactory sessionFactory;
  private final SessionProvider sessionProvider;

  public SessionServiceProvider(SessionFactory sessionFactory, SessionProvider sessionProvider) {
    this.sessionFactory = sessionFactory;
    this.sessionProvider = sessionProvider;
  }

  @Override public Session get() {
    return ManagedSessionContext.hasBind(sessionFactory)
        ? sessionFactory.getCurrentSession()
        : sessionProvider.newSession(sessionFactory.withOptions());
  }
}
