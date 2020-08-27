/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.hibernate;

import io.jooby.SneakyThrows;
import io.jooby.hibernate.UnitOfWork;
import org.hibernate.*;
import org.hibernate.context.internal.ManagedSessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;

import static java.util.Objects.requireNonNull;

public class UnitOfWorkImpl implements UnitOfWork {

  private final Session session;
  private final Logger logger = LoggerFactory.getLogger(UnitOfWorkImpl.class);

  public UnitOfWorkImpl(Session session) {
    this.session = requireNonNull(session);
    bind(session);
    session.setHibernateFlushMode(FlushMode.AUTO);
  }

  private void bind(Session session) {
    logger.debug("session bound: {}", oid(session));
    ManagedSessionContext.bind(session);
  }

  private void unbind(SessionFactory sessionFactory) {
    final Session session = ManagedSessionContext.unbind(sessionFactory);
    logger.debug("session unbound: {}", oid(session));
  }

  private String oid(Object value) {
    return Integer.toHexString(System.identityHashCode(value));
  }

  private void begin() {
    final Transaction tx = session.getTransaction();
    logger.debug("begin transaction: {}(trx@{})", oid(session), oid(tx));
    tx.begin();
  }

  private void commit() {
    logger.debug("flushing session: {}", oid(session));
    session.flush();

    final Transaction tx = session.getTransaction();
    logger.debug("commit transaction: {}(trx@{})", oid(session), oid(tx));
    tx.commit();
  }

  private void rollback() {
    final Transaction tx = session.getTransaction();
    logger.debug("rollback transaction: {}(trx@{})", oid(session), oid(tx));
    tx.rollback();
  }

  @Override
  public <T> T apply(SneakyThrows.Function2<EntityManager, TransactionHandler, T> callback) {
    if (!session.isOpen()) {
      throw new IllegalStateException("Session is not open.");
    }

    try {
      begin();

      final T result = callback.apply(session, new TransactionHandler() {

        @Override
        public void commit() {
          UnitOfWorkImpl.this.commit();
          begin();
        }

        @Override
        public void rollback() {
          UnitOfWorkImpl.this.rollback();
          begin();
        }
      });

      commit();

      return result;
    } catch (Throwable t) {
      try {
        rollback();
      } catch (Throwable th) {
        logger.error("failed to rollback transaction: {}", oid(session), th);
      }

      throw SneakyThrows.propagate(t);
    } finally {
      try {
        session.close();
        logger.debug("session closed: {}", oid(session));
      } catch (HibernateException e) {
        logger.error("session.close() resulted in exception: {}", oid(session), e);
      } finally {
        unbind(session.getSessionFactory());
      }
    }
  }
}
