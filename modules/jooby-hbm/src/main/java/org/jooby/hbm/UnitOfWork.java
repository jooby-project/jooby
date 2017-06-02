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

import javax.persistence.EntityManager;

import org.hibernate.Session;

import javaslang.control.Try.CheckedConsumer;
import javaslang.control.Try.CheckedFunction;

/**
 * <h2>unit of work</h2>
 * <p>
 * We provide an {@link UnitOfWork} to simplify the amount of code required to interact within the
 * database.
 * </p>
 * <p>
 * For example the next line:
 * </p>
 *
 * <pre>{@code
 * {
 *   require(UnitOfWork.class).apply(em -> {
 *     return em.createQuery("from Beer").getResultList();
 *   });
 * }
 * }</pre>
 *
 * <p>
 * Is the same as:
 * </p>
 *
 * <pre>{@code
 * {
 *    Session session = require(SessionFactory.class).openSession();
 *    Transaction trx = session.getTransaction();
 *    try {
 *      trx.begin();
 *      List<Beer> beers = em.createQuery("from Beer").getResultList();
 *      trx.commit();
 *    } catch (Exception ex) {
 *      trx.rollback();
 *    } finally {
 *      session.close();
 *    }
 * }
 * }</pre>
 *
 * <p>
 * An {@link UnitOfWork} takes care of transactions and session life-cycle. It's worth to mention
 * too that a first requested {@link UnitOfWork} bind the Session to the current request. If later
 * in the execution flow an {@link UnitOfWork}, {@link Session} and/or {@link EntityManager} is
 * required then the one that belong to the current request (first requested) will be provided it.
 * </p>
 *
 * @author edgar
 * @since 1.0.0.CR7
 */
public interface UnitOfWork {

  /**
   * Get access to a {@link Session}/{@link EntityManager} and do some work.
   *
   * @param callback Callback to run.
   * @throws Throwable If something goes wrong.
   */
  default void accept(final CheckedConsumer<Session> callback) throws Throwable {
    apply(session -> {
      callback.accept(session);
      return null;
    });
  }

  /**
   * Get access to a {@link Session}/{@link EntityManager}, do some work and returns some value.
   *
   * @param callback Callback to run.
   * @param <T> Return type.
   * @return Returns value.
   * @throws Throwable If something goes wrong.
   */
  <T> T apply(final CheckedFunction<Session, T> callback) throws Throwable;
}
