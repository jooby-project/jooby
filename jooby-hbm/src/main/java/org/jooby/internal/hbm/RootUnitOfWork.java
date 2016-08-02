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

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.context.internal.ManagedSessionContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.jooby.hbm.UnitOfWork;

import javaslang.control.Try;
import javaslang.control.Try.CheckedFunction;

public class RootUnitOfWork extends AbstractUnitOfWork {

  private volatile boolean rollbackOnly;

  private volatile boolean readOnly;

  public RootUnitOfWork(final Session session) {
    super(session);
    bind(session);
    session.setHibernateFlushMode(FlushMode.AUTO);
  }

  public UnitOfWork begin() {
    if (rollbackOnly) {
      return this;
    }
    active(session, trx -> {
      log.debug("joining existing transaction: {}(trx@{})", oid(session), oid(trx));
    }, trx -> {
      log.debug("begin transaction: {}(trx@{})", oid(session), oid(trx));
      trx.begin();
    });

    return this;
  }

  public UnitOfWork commit() {
    if (rollbackOnly) {
      return this;
    }
    if (!readOnly) {
      log.debug("flusing session: {}", oid(session));
      session.flush();
    } else {
      log.debug("flusing ignored on read-only session: {}", oid(session));
    }
    active(session, trx -> {
      log.debug("commiting transaction: {}(trx@{})", oid(session), oid(trx));
      trx.commit();
    }, trx -> {
      log.warn("unable to commit inactive transaction: {}(trx@{})", oid(session), oid(trx));
    });
    return this;
  }

  public RootUnitOfWork setRollbackOnly() {
    this.rollbackOnly = true;
    return this;
  }

  public RootUnitOfWork setReadOnly() {
    if (rollbackOnly) {
      return this;
    }

    log.debug("read-only session: {}", oid(session));
    setConnectionReadOnly(true);
    readOnly = true;
    session.setHibernateFlushMode(FlushMode.MANUAL);
    session.setDefaultReadOnly(true);
    return this;
  }

  public UnitOfWork rollback() {
    active(session, trx -> {
      log.debug("rollback transaction: {}(trx@{})", oid(session), oid(trx));
      trx.rollback();
    }, trx -> {
      log.warn("unable to rollback inactive transaction: {}(trx@{})", oid(session), oid(trx));
    });
    return this;
  }

  @Override
  public <T> T apply(final CheckedFunction<Session, T> callback) throws Throwable {
    try {
      begin();
      T value = callback.apply(session);
      return value;
    } catch (Throwable x) {
      rollbackOnly = true;
      throw x;
    } finally {
      end();
    }
  }

  public void end() {
    try {
      if (rollbackOnly) {
        rollback();
      } else {
        commit();
      }
    } finally {
      if (readOnly) {
        setConnectionReadOnly(false);
      }

      String sessionId = oid(session);
      log.debug("closing session: {}", sessionId);
      Try.run(session::close)
          .onFailure(x -> log.error("session.close() resulted in exception: {}", sessionId, x))
          .onSuccess(v -> log.debug("session closed: {}", sessionId));
      unbind(session.getSessionFactory());
    }
  }

  protected void bind(final Session session) {
    log.debug("session bound: {}", oid(session));
    ManagedSessionContext.bind(session);
  }

  protected void unbind(final SessionFactory sessionFactory) {
    Session s = ManagedSessionContext.unbind(sessionFactory);
    log.debug("session unbound: {}", oid(s));
  }

  private void setConnectionReadOnly(final boolean readonly) {
    try {
      Connection connection = ((SessionImplementor) session).connection();
      connection.setReadOnly(readonly);
    } catch (Exception ex) {
      log.trace("session connection.setReadOnly({}) failed: {}", readonly, oid(session), ex);
    }
  }

}
