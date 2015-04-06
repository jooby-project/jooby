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
      session.setDefaultReadOnly(readOnly);
    };

    EntityTransaction trx = em.getTransaction();
    try {
      log.debug("  [{}] flushing", sessionId);
      session.flush();

      if (trx.isActive()) {
        log.debug("  [{}] commiting transaction: {}", sessionId, trx);
        trx.commit();
      }

      EntityTransaction readOnlyTrx = null;
      try {
        // start new transaction
        log.debug("  [{}] setting connection to read only", sessionId);
        setReadOnly.accept(true);
        session.setFlushMode(FlushMode.MANUAL);
        readOnlyTrx = em.getTransaction();
        log.debug("  [{}] starting readonly transaction: {}", sessionId, readOnlyTrx);
        readOnlyTrx.begin();

        // send it!
        super.send(result);

        log.debug("  [{}] commiting readonly transaction: {}", sessionId, readOnlyTrx);
        readOnlyTrx.commit();
      } catch (Exception ex) {
        if (readOnlyTrx != null && readOnlyTrx.isActive()) {
          log.debug("  [{}] rolling back readonly transaction: {}", sessionId, readOnlyTrx);
          readOnlyTrx.rollback();
        }
        throw ex;
      } finally {
        log.debug("  [{}] removing readonly mode from connection", sessionId);
        setReadOnly.accept(false);
      }
    } catch (Exception ex) {
      if (trx.isActive()) {
        log.debug("  [{}] rolling back transation: {}", sessionId, trx);
        trx.rollback();
      }
      throw ex;
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
