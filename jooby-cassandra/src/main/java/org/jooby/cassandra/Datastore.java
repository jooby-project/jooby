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
package org.jooby.cassandra;

import java.util.Map;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.Result;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * <h1>datastore</h1>
 * <p>
 * Provides basic crud operations, like a {@link Mapper} without requiring an instance per each
 * entity class.
 * </p>
 *
 * @author edgar
 * @since 1.0.0.CR7
 */
@SuppressWarnings({"rawtypes", "unchecked" })
public class Datastore {
  private MappingManager manager;

  Datastore(final MappingManager manager) {
    this.manager = manager;
  }

  private <T> Mapper<T> mapper(final Class<T> entityClass) {
    return manager.mapper(entityClass);
  }

  /**
   * Fetch an entity based on its primary key.
   *
   * @param entityClass Entity class.
   * @param id Object id.
   * @return An entity or <code>null</code>.
   */
  public <T> T get(final Class<T> entityClass, final Object id) {
    return mapper(entityClass).get(id);
  }

  /**
   * Fetch an entity based on its primary key.
   *
   * @param entityClass Entity class.
   * @param id Object id.
   * @return A ListenableFuture containing an entity or <code>null</code>
   */
  public <T> ListenableFuture<T> getAsync(final Class<T> entityClass, final Object id) {
    return mapper(entityClass).getAsync(id);
  }

  /**
   * Delete an entity by ID.
   *
   * @param entityClass Entity class.
   * @param id entity ID.
   * @param options Options.
   */
  public void delete(final Class<?> entityClass, final Object id, final Mapper.Option... options) {
    Mapper mapper = mapper(entityClass);
    mapper.delete(id, options);
  }

  /**
   * Delete an entity by ID.
   *
   * @param entityClass Entity class.
   * @param id Entity id.
   * @param options Options.
   * @return An empty listenable future.
   */
  public ListenableFuture<Void> deleteAsync(final Class<?> entityClass, final Object id,
      final Mapper.Option... options) {
    Mapper mapper = mapper(entityClass);
    return mapper.deleteAsync(id, options);
  }

  /**
   * Delete an entity.
   *
   * @param entity Entity to delete.
   * @param options Options.
   */
  public void delete(final Object entity, final Mapper.Option... options) {
    Mapper mapper = mapper(entity.getClass());
    mapper.delete(entity, options);
  }

  /**
   * Delete an entity.
   *
   * @param entity Entity to delete.
   * @param options Options.
   * @return An empty listenable future.
   */
  public ListenableFuture<Void> deleteAsync(final Object entity, final Mapper.Option... options) {
    Mapper mapper = mapper(entity.getClass());
    return mapper.deleteAsync(entity, options);
  }

  /**
   * Save an entity.
   *
   * @param entity Entity to save.
   * @param options Options.
   */
  public <T> void save(final T entity, final Mapper.Option... options) {
    Class entityClass = entity.getClass();
    Mapper mapper = mapper(entityClass);
    mapper.save(entity, options);
  }

  /**
   * Save an entity.
   *
   * @param entity Entity to save.
   * @param options Options.
   * @return An empty listenable future.
   */
  public <T> ListenableFuture<Void> saveAsync(final T entity, final Mapper.Option... options) {
    Class entityClass = entity.getClass();
    Mapper mapper = mapper(entityClass);
    return mapper.saveAsync(entity, options);
  }

  /**
   * Execute a query and map result to entityClass.
   *
   * @param entityClass Entity class.
   * @param statement Statement to execute.
   * @param values Statement parameters.
   * @return A query result.
   */
  public <T> Result<T> query(final Class<T> entityClass, final String statement,
      final Map<String, Object> values) {
    return query(entityClass, new SimpleStatement(statement, values));
  }

  /**
   * Execute a query and map result to entityClass.
   *
   * @param entityClass Entity class.
   * @param statement Statement to execute.
   * @param values Statement parameters.
   * @return A query result.
   */
  public <T> Result<T> query(final Class<T> entityClass, final String statement,
      final Object... values) {
    return query(entityClass, new SimpleStatement(statement, values));
  }

  /**
   * Execute a query and map result to entityClass.
   *
   * @param entityClass Entity class.
   * @param statement Statement to execute.
   * @return A query result.
   */
  public <T> Result<T> query(final Class<T> entityClass, final Statement statement) {
    Mapper<T> mapper = mapper(entityClass);
    Session session = mapper.getManager().getSession();
    ResultSet rs = session.execute(statement);
    return mapper.map(rs);
  }

  /**
   * Execute a query and map result to entityClass.
   *
   * @param entityClass Entity class.
   * @param statement Statement to execute.
   * @param values Statement parameters.
   * @return A listenable future holding the result.
   */
  public <T> ListenableFuture<Result<T>> queryAsync(final Class<T> entityClass,
      final String statement, final Map<String, Object> values) {
    return queryAsync(entityClass, new SimpleStatement(statement, values));
  }

  /**
   * Execute a query and map result to entityClass.
   *
   * @param entityClass Entity class.
   * @param statement Statement to execute.
   * @param values Statement parameters.
   * @return A listenable future holding the result.
   */
  public <T> ListenableFuture<Result<T>> queryAsync(final Class<T> entityClass,
      final String statement, final Object... values) {
    return queryAsync(entityClass, new SimpleStatement(statement, values));
  }

  /**
   * Execute a query and map result to entityClass.
   *
   * @param entityClass Entity class.
   * @param statement Statement to execute.
   * @return A listenable future holding the result.
   */
  public <T> ListenableFuture<Result<T>> queryAsync(final Class<T> entityClass,
      final Statement statement) {
    Mapper<T> mapper = mapper(entityClass);
    Session session = mapper.getManager().getSession();
    ResultSetFuture rs = session.executeAsync(statement);
    return Futures.transformAsync(rs, rs1 -> Futures.immediateFuture(mapper.map(rs1)));
  }
}
