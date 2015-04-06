package org.jooby.hbm;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.context.internal.ManagedSessionContext;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Route.Chain;
import org.jooby.internal.hbm.HbmProvider;
import org.jooby.internal.hbm.TrxResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Key;

public class OpenSessionInView implements Route.Filter {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private HbmProvider emf;

  private List<Key<EntityManager>> keys;

  public OpenSessionInView(final HbmProvider emf, final List<Key<EntityManager>> keys) {
    this.emf = emf;
    this.keys = keys;
  }

  @Override
  public void handle(final Request req, final Response rsp, final Chain chain) throws Exception {
    HibernateEntityManagerFactory hemf = emf.get();
    SessionFactory sf = hemf.getSessionFactory();

    EntityManager em = hemf.createEntityManager();
    Session session = (Session) em.getDelegate();
    String sessionId = Integer.toHexString(System.identityHashCode(session));

    log.debug("session opened: {}", sessionId);
    log.debug("  [{}] binding", sessionId);

    ManagedSessionContext.bind(session);

    keys.forEach(key -> req.set(key, em));
    FlushMode flushMode = FlushMode.AUTO;
    log.debug("  [{}] flush mode: {}", sessionId, flushMode);
    session.setFlushMode(flushMode);
    EntityTransaction trx = em.getTransaction();
    try {
      log.debug("  [{}] starting transation: {}", sessionId, trx);
      trx.begin();

      // invoke next handler
      chain.next(req, new TrxResponse(rsp, em));
    } finally {
      try {
        if (trx.isActive()) {
          log.debug("  [{}] rolling back transation: {}", sessionId, trx);
          trx.rollback();
        }
      } finally {
        try {
          log.debug("  [{}] closing", sessionId);
          em.close();
        } finally {
          log.debug("  [{}] unbinding", sessionId);
          ManagedSessionContext.unbind(sf);
          log.debug("session released: [{}]", sessionId);
        }
      }
    }
  }

}
