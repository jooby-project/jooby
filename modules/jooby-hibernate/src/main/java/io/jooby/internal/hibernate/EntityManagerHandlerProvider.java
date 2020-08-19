package io.jooby.internal.hibernate;

import io.jooby.hibernate.EntityManagerHandler;
import io.jooby.hibernate.SessionProvider;
import io.jooby.hibernate.SessionRequest;
import io.jooby.hibernate.TransactionalRequest;
import org.hibernate.SessionFactory;
import org.hibernate.context.internal.ManagedSessionContext;

import javax.inject.Provider;

public class EntityManagerHandlerProvider implements Provider<EntityManagerHandler> {

  private final SessionFactory sessionFactory;
  private final SessionProvider sessionProvider;

  public EntityManagerHandlerProvider(SessionFactory sessionFactory, SessionProvider sessionProvider) {
    this.sessionFactory = sessionFactory;
    this.sessionProvider = sessionProvider;
  }

  @Override
  public EntityManagerHandler get() {
    if (ManagedSessionContext.hasBind(sessionFactory)) {
      throw new IllegalStateException("A session is already bound to the current thread. Don't nest "
          + EntityManagerHandler.class.getSimpleName() + " invocations or don't use "
          + EntityManagerHandler.class.getSimpleName() + " together with "
          + SessionRequest.class.getSimpleName() + " or "
          + TransactionalRequest.class.getSimpleName() + ".");
    }

    return new EntityManagerHandlerImpl(sessionProvider.newSession(sessionFactory.withOptions()));
  }
}
