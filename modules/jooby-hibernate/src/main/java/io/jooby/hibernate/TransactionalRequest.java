/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.hibernate;

import io.jooby.Route;
import io.jooby.ServiceKey;
import io.jooby.Sneaky;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;

import javax.annotation.Nonnull;

public class TransactionalRequest implements Route.Decorator {

  private ServiceKey<SessionFactory> key;

  public TransactionalRequest(@Nonnull String name) {
    key = ServiceKey.key(SessionFactory.class, name);
  }

  public TransactionalRequest() {
    key = ServiceKey.key(SessionFactory.class);
  }

  @Nonnull @Override public Route.Handler apply(@Nonnull Route.Handler next) {
    return ctx -> {
      SessionFactory sessionFactory = ctx.require(key);
      Transaction trx = null;
      try {
        Session session = sessionFactory.openSession();
        ManagedSessionContext.bind(session);
        trx = session.getTransaction();
        trx.begin();

        Object result = next.apply(ctx);

        if (trx.isActive()) {
          trx.commit();
        }
        return result;
      } catch (Throwable x) {
        if (trx != null && trx.isActive()) {
          trx.rollback();
        }
        throw Sneaky.propagate(x);
      } finally {
        Session session = ManagedSessionContext.unbind(sessionFactory);
        if (session != null) {
          session.close();
        }
      }
    };
  }
}
