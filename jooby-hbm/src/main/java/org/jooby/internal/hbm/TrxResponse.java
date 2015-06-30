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

  private Session session;

  private EntityManager em;

  private String sessionId;

  private EntityTransaction trx;

  private boolean rollbackOnly;

  public TrxResponse(final Response response, final EntityManager em) {
    super(response);
    this.em = em;
    this.session = (Session) em.getDelegate();
    this.sessionId = Integer.toHexString(System.identityHashCode(session));
  }

  public TrxResponse begin() {
    this.trx = em.getTransaction();

    log.debug("  [{}] starting transation: {}", sessionId, trx);

    trx.begin();

    return this;
  }

  public void setRollbackOnly() {
    rollbackOnly = true;
  }

  private void trxSend(final Object result) throws Exception {

    Consumer<Boolean> setReadOnly = (readOnly) -> {
      try {
        Connection connection = ((SessionImplementor) session).connection();
        connection.setReadOnly(readOnly);
      } catch (Exception ex) {
        log.trace("  [" + sessionId + "] unable to setReadOnly " + readOnly, ex);
      }
      session.setDefaultReadOnly(readOnly);
    };

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
      rollbackOnly = true;
      throw ex;
    } finally {
      done();
    }
  }

  public void done() {
    EntityManager em = this.em;
    if (em == null) {
      return;
    }
    EntityTransaction trx = this.trx;
    this.em = null;
    this.session = null;
    this.trx = null;
    try {
      if (trx.isActive()) {
        if (rollbackOnly) {
          log.debug("  [{}] rolling back transation: {}", sessionId, trx);
          trx.rollback();
        } else {
          log.debug("  [{}] commiting transaction: {}", sessionId, trx);
          trx.commit();
        }
      }
    } catch (Exception ex) {
      log.error("  [" + sessionId + "] unable trying to commit/rollback trx resulted in error", ex);
    } finally {
      log.debug("  [{}] closing", sessionId);
      em.close();
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
