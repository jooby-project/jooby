package org.jooby.internal.hbm;

import java.sql.Connection;
import java.util.function.Consumer;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.jooby.Response;
import org.jooby.Result;
import org.jooby.hbm.OpenSessionInView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrxResponse extends Response.Forwarding {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(OpenSessionInView.class);

  private EntityManager em;

  public TrxResponse(final Response response, final EntityManager em) {
    super(response);
    this.em = em;
  }

  private void trxSend(final Object result) throws Exception {
    Session session = (Session) em.getDelegate();
    String sessionId = Integer.toHexString(System.identityHashCode(session));

    Consumer<Boolean> setReadOnly = (readOnly) -> {
      try {
        Connection connection = ((SessionImplementor) session).connection();
        connection.setReadOnly(readOnly);
      } catch (Exception ex) {
        log.trace("  [" + sessionId + "] unable to setReadOnly " + readOnly, ex);
      }
      session.setDefaultReadOnly(true);
    };

    EntityTransaction trx = em.getTransaction();
    try {
      log.debug("  [{}] flushing", sessionId);
      session.flush();

      if (trx.isActive()) {
        log.debug("  [{}] commiting transaction: {}", sessionId, trx);
        trx.commit();
      }

      // start new transaction
      log.debug("  [{}] setting connection to read only", sessionId);
      setReadOnly.accept(true);
      session.setFlushMode(FlushMode.MANUAL);
      EntityTransaction readOnlyTrx = em.getTransaction();
      log.debug("  [{}] starting readonly transaction: {}", sessionId, readOnlyTrx);
      readOnlyTrx.begin();

      try {
        // send it!
        super.send(result);

        log.debug("  [{}] commiting readonly transaction: {}", sessionId, readOnlyTrx);
        readOnlyTrx.commit();
      } catch (Exception ex) {
        if (readOnlyTrx.isActive()) {
          log.debug("  [{}] rolling back readonly transaction: {}", sessionId, readOnlyTrx);
          readOnlyTrx.rollback();
        }
        throw ex;
      }
    } finally {
      log.debug("  [{}] removing readonly mode from connection", sessionId);
      setReadOnly.accept(false);
    }
  }

  @Override
  public void send(final Object result) throws Exception {
    trxSend(result);
  }

  @Override
  public void send(final Result result) throws Exception {
    trxSend(result);
  }
}
