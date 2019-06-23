/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.hibernate;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.context.internal.ManagedSessionContext;

import javax.inject.Provider;

public class SessionProvider implements Provider<Session> {
  private final SessionFactory sessionFactory;

  public SessionProvider(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override public Session get() {
    return ManagedSessionContext.hasBind(sessionFactory)
        ? sessionFactory.getCurrentSession()
        : sessionFactory.openSession();
  }
}
