/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.hibernate;

import io.jooby.hibernate.UnitOfWork;
import io.jooby.hibernate.SessionProvider;
import io.jooby.hibernate.SessionRequest;
import io.jooby.hibernate.TransactionalRequest;
import org.hibernate.SessionFactory;
import org.hibernate.context.internal.ManagedSessionContext;

import javax.inject.Provider;

public class UnitOfWorkProvider implements Provider<UnitOfWork> {

  private final SessionFactory sessionFactory;
  private final SessionProvider sessionProvider;

  public UnitOfWorkProvider(SessionFactory sessionFactory, SessionProvider sessionProvider) {
    this.sessionFactory = sessionFactory;
    this.sessionProvider = sessionProvider;
  }

  @Override
  public UnitOfWork get() {
    if (ManagedSessionContext.hasBind(sessionFactory)) {
      throw new IllegalStateException("A session is already bound to the current thread. Don't nest "
          + UnitOfWork.class.getSimpleName() + " invocations or don't use "
          + UnitOfWork.class.getSimpleName() + " together with "
          + SessionRequest.class.getSimpleName() + " or "
          + TransactionalRequest.class.getSimpleName() + ".");
    }

    return new UnitOfWorkImpl(sessionProvider.newSession(sessionFactory.withOptions()));
  }
}
