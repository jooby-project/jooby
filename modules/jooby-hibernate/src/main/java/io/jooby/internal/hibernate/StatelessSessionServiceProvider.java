/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;

import io.jooby.RequestScope;
import io.jooby.hibernate.StatelessSessionProvider;
import jakarta.inject.Provider;

public class StatelessSessionServiceProvider implements Provider<StatelessSession> {
  private final SessionFactory sessionFactory;
  private final StatelessSessionProvider sessionProvider;

  public StatelessSessionServiceProvider(
      SessionFactory sessionFactory, StatelessSessionProvider sessionProvider) {
    this.sessionFactory = sessionFactory;
    this.sessionProvider = sessionProvider;
  }

  @Override
  public StatelessSession get() {
    return RequestScope.hasBind(sessionFactory)
        ? RequestScope.get(sessionFactory)
        : sessionProvider.newSession(sessionFactory.withStatelessOptions());
  }
}
