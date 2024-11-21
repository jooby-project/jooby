/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionContract;
import org.hibernate.context.internal.ManagedSessionContext;

import io.jooby.Context;
import io.jooby.RequestScope;
import io.jooby.ServiceKey;
import io.jooby.hibernate.SessionProvider;
import io.jooby.hibernate.StatelessSessionProvider;

public abstract class RequestSessionFactory {
  public abstract SharedSessionContract create(Context ctx, SessionFactory sessionFactory);

  public abstract void release(SessionFactory sessionFactory);

  public static RequestSessionFactory stateless(ServiceKey<StatelessSessionProvider> key) {
    return new StatelessSessionFactory(key);
  }

  public static RequestSessionFactory stateful(ServiceKey<SessionProvider> key) {
    return new StatefulSessionFactory(key);
  }

  private static class StatefulSessionFactory extends RequestSessionFactory {
    private final ServiceKey<SessionProvider> sessionProviderKey;

    public StatefulSessionFactory(ServiceKey<SessionProvider> sessionProviderKey) {
      this.sessionProviderKey = sessionProviderKey;
    }

    @Override
    public SharedSessionContract create(Context ctx, SessionFactory sessionFactory) {
      var sessionProvider = ctx.require(sessionProviderKey);
      var session = sessionProvider.newSession(sessionFactory.withOptions());
      ManagedSessionContext.bind(session);
      return session;
    }

    @Override
    public void release(SessionFactory sessionFactory) {
      ManagedSessionContext.unbind(sessionFactory);
    }
  }

  private static class StatelessSessionFactory extends RequestSessionFactory {
    private final ServiceKey<StatelessSessionProvider> sessionProviderKey;

    public StatelessSessionFactory(ServiceKey<StatelessSessionProvider> sessionProviderKey) {
      this.sessionProviderKey = sessionProviderKey;
    }

    @Override
    public SharedSessionContract create(Context ctx, SessionFactory sessionFactory) {
      var sessionProvider = ctx.require(sessionProviderKey);
      var session = sessionProvider.newSession(sessionFactory.withStatelessOptions());
      RequestScope.bind(sessionFactory, session);
      return session;
    }

    @Override
    public void release(SessionFactory sessionFactory) {
      RequestScope.unbind(sessionFactory);
    }
  }
}
