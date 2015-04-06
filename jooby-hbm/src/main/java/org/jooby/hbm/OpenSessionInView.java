/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.hbm;

import java.util.List;

import javax.inject.Provider;
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
import org.jooby.internal.hbm.TrxResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Key;

public class OpenSessionInView implements Route.Filter {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private Provider<HibernateEntityManagerFactory> emf;

  private List<Key<EntityManager>> keys;

  public OpenSessionInView(final Provider<HibernateEntityManagerFactory> emf,
      final List<Key<EntityManager>> keys) {
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
    keys.forEach(key -> req.set(key, em));

    log.debug("session opened: {}", sessionId);
    EntityTransaction trx = em.getTransaction();
    try {
      log.debug("  [{}] binding", sessionId);
      ManagedSessionContext.bind(session);

      FlushMode flushMode = FlushMode.AUTO;
      log.debug("  [{}] flush mode: {}", sessionId, flushMode);
      session.setFlushMode(flushMode);

      log.debug("  [{}] starting transation: {}", sessionId, trx);
      trx.begin();

      // invoke next handler
      chain.next(req, new TrxResponse(rsp, em));
    } finally {
      closeUnbind(sf, em, sessionId);
    }
  }

  private void closeUnbind(final SessionFactory sf, final EntityManager em,
      final String sessionId) {
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
