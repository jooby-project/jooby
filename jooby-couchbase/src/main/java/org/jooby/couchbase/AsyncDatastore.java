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
package org.jooby.couchbase;

import java.util.List;

import com.couchbase.client.core.message.kv.MutationToken;
import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.PersistTo;
import com.couchbase.client.java.ReplicaMode;
import com.couchbase.client.java.ReplicateTo;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.error.TemporaryLockFailureException;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.repository.AsyncRepository;
import com.couchbase.client.java.repository.annotation.Id;
import com.couchbase.client.java.view.ViewQuery;

import rx.Observable;

/**
 * <h1>datastore</h1>
 * <p>
 * Create, read, update and delete entities from an {@link AsyncBucket}. It is similar to
 * {@link AsyncRepository} but less verbose or more ready to use, but also add support for
 * {@link ViewQuery} and {@link N1qlQuery}.
 * </p>
 *
 * <h2>design/implementation choices</h2>
 * <p>
 * In order to abstract developers from doing basic CRUD operations, the following design has been
 * made:
 * </p>
 *
 * <ul>
 * <li>IDs look like: <code>classname::id</code>.</li>
 * <li>ID is also stored within the document as an attribute</li>
 * <li>A <code>class</code> attribute is also stored within the document</li>
 * </ul>
 *
 * <p>
 * Here is an example of a document for a class: <code>model.Beer</code>:
 * </p>
 * <pre>{@code
 * {
 *   "model.Beer::678": {
 *     "name": "IPA",
 *     "id": 678,
 *     "class": "model.Beer"
 *   }
 * }
 * }</pre>
 *
 * <h2>ID selection</h2>
 * <p>
 * In order to mark a class field as document ID, you must:
 * </p>
 * <ul>
 * <li>Name the field: <code>id</code>, or</li>
 * <li>Annotated the field with {@link Id}</li>
 * </ul>
 *
 * <p>
 * Auto-increment ID are supported by annotating the field with {@link GeneratedValue} and declare
 * the field as {@link Long}. Then whenever you make a call to {@link #insert(Object)} or
 * {@link #upsert(Object)} a new ID will be generated if need it.
 * </p>
 *
 * @author edgar
 * @since 1.0.0.CR7
 */
public interface AsyncDatastore {

  /**
   * Result of {@link AsyncDatastore#query(ViewQuery)} contains a list of entities plus totalRows in
   * the view.
   *
   * @author edgar
   * @param <T> Entity type.
   */
  class AsyncViewQueryResult<T> {

    /** Total number of rows in the view. */
    private int totalRows;

    /** List of rows from current execution. */
    private Observable<List<T>> rows;

    /**
     * Creates a new {@link AsyncViewQueryResult}.
     *
     * @param totalRows Total number of rows in the view.
     * @param rows Resultset from current execution.
     */
    public AsyncViewQueryResult(final int totalRows, final Observable<List<T>> rows) {
      this.totalRows = totalRows;
      this.rows = rows;
    }

    /**
     * @return Total number of rows in the view.
     */
    public int getTotalRows() {
      return totalRows;
    }

    /**
     * @return Resultset from current execution.
     */
    public Observable<List<T>> getRows() {
      return rows;
    }

  }

  /**
   * Provides advanced options for couchbase operation.
   *
   * @author edgar
   */
  interface AsyncCommand {
    /**
     * Set the expiry/ttl entity option.
     *
     * @param expiry The expiration time expressed as relative seconds from now.
     * @return This command.
     */
    AsyncCommand expiry(int expiry);

    /**
     * Set a CAS value for the entity (0 if not set).
     *
     * @param cas Cas value.
     * @return This command.
     */
    AsyncCommand cas(long cas);

    /**
     * The optional, opaque mutation token set after a successful mutation and if enabled on
     * the environment.
     *
     * Note that the mutation token is always null, unless they are explicitly enabled on the
     * environment, the server version is supported (&gt;= 4.0.0) and the mutation operation succeeded.
     *
     * If set, it can be used for enhanced durability requirements, as well as optimized consistency
     * for N1QL queries.
     *
     * @param mutationToken the mutation token if set, otherwise null.
     * @return This command.
     */
    AsyncCommand mutationToken(MutationToken mutationToken);

    /**
     * Execute this command.
     *
     * @param entity Entity to use.
     * @param <R> Entity type.
     * @return Command result.
     */
    default <R> Observable<R> execute(final Object entity) {
      return execute(entity, PersistTo.NONE);
    }

    /**
     * Execute this command.
     *
     * @param entity Entity to use.
     * @param persistTo Persist to value.
     * @param <R> Entity type.
     * @return Command result.
     */
    default <R> Observable<R> execute(final Object entity, final PersistTo persistTo) {
      return execute(entity, persistTo, ReplicateTo.NONE);
    }

    /**
     * Execute this command.
     *
     * @param entity Entity to use.
     * @param replicateTo Replicate to value.
     * @param <R> Entity type.
     * @return Command result.
     */
    default <R> Observable<R> execute(final Object entity, final ReplicateTo replicateTo) {
      return execute(entity, PersistTo.NONE, replicateTo);
    }

    /**
     * Execute this command.
     *
     * @param entity Entity to use.
     * @param persistTo Persist to value.
     * @param replicateTo Replicate to value.
     * @param <R> Entity type.
     * @return Command result.
     */
    <R> Observable<R> execute(Object entity, PersistTo persistTo, ReplicateTo replicateTo);
  }

  /**
   * Provides advanced options for couchbase operation.
   *
   * @author edgar
   * @since 1.0.0.CR7
   */
  @SuppressWarnings("unchecked")
  interface AsyncRemoveCommand extends AsyncCommand {

    /**
     * Execute a remove document operation
     *
     * @param entity Entity to remove.
     * @param persistTo Persist to option.
     * @param replicateTo Replicate to option.
     * @return CAS value.
     */
    @Override
    Observable<Long> execute(Object entity, PersistTo persistTo, ReplicateTo replicateTo);

    /**
     * Execute a remove document operation
     *
     * @param entityClass Entity class to remove.
     * @param id Entity id to remove.
     * @return CAS value.
     */
    default Observable<Long> execute(final Class<?> entityClass, final Object id) {
      return execute(entityClass, id, PersistTo.NONE);
    }

    /**
     * Execute a remove document operation
     *
     * @param entityClass Entity class to remove.
     * @param id Entity id to remove.
     * @param persistTo Persist to option.
     * @return CAS value.
     */
    default Observable<Long> execute(final Class<?> entityClass, final Object id,
        final PersistTo persistTo) {
      return execute(entityClass, id, persistTo, ReplicateTo.NONE);
    }

    /**
     * Execute a remove document operation
     *
     * @param entityClass Entity class to remove.
     * @param id Entity id to remove.
     * @param replicateTo Replicate to option.
     * @return CAS value.
     */
    default Observable<Long> execute(final Class<?> entityClass, final Object id,
        final ReplicateTo replicateTo) {
      return execute(entityClass, id, PersistTo.NONE, replicateTo);
    }

    /**
     * Execute a remove document operation
     *
     * @param entityClass Entity class to remove.
     * @param id Entity id to remove.
     * @param persistTo Persist to option.
     * @param replicateTo Replicate to option.
     * @return CAS value.
     */
    Observable<Long> execute(final Class<?> entityClass, Object id, PersistTo persistTo,
        ReplicateTo replicateTo);
  }

  /**
   * Get an entity/document by ID. The unique ID is constructed via
   * {@link N1Q#qualifyId(Class, Object)}.
   *
   * @param entityClass Entity class.
   * @param id Entity id.
   * @param <T> Entity type.
   * @return An observable entity matching the id or an empty observable.
   */
  <T> Observable<T> get(Class<T> entityClass, Object id);

  /**
   * Get an entity/document by ID. The unique ID is constructed via
   * {@link N1Q#qualifyId(Class, Object)}.
   *
   * @param entityClass Entity class.
   * @param id Entity id.
   * @param mode Replica mode.
   * @param <T> Entity type.
   * @return An observable entity matching the id or an empty observable.
   */
  <T> Observable<T> getFromReplica(Class<T> entityClass, Object id, ReplicaMode mode);

  /**
   * Retrieve and lock a entity by its unique ID.
   *
   * If the entity is found, a entity is returned. If the entity is not found, the
   * {@link Observable} completes without an item emitted (empty).
   *
   * This method works similar to {@link #get(Class, Object)}, but in addition it (write) locks the
   * entity for the given lock time interval. Note that this lock time is hard capped to 30
   * seconds, even if provided with a higher value and is not configurable. The entity will unlock
   * afterwards automatically.
   *
   * Detecting an already locked entity is done by checking for
   * {@link TemporaryLockFailureException}. Note that this exception can also be raised in other
   * conditions, always when the error is transient and retrying may help.
   *
   * @param entityClass Entity class.
   * @param id id the unique ID of the entity.
   * @param lockTime the time to write lock the entity (max. 30 seconds).
   * @param <T> Entity type.
   * @return an {@link Observable} eventually containing the found {@link JsonDocument}.
   */
  <T> Observable<T> getAndLock(Class<T> entityClass, Object id, int lockTime);

  /**
   * Retrieve and touch an entity by its unique ID.
   *
   * If the entity is found, an entity is returned. If the entity is not found, the
   * {@link Observable} completes without an item emitted (empty).
   *
   * This method works similar to {@link #get(Class, Object)}, but in addition it touches the
   * entity, which will reset its configured expiration time to the value provided.
   *
   * @param entityClass Entity class.
   * @param id id the unique ID of the entity.
   * @param expiry the new expiration time for the entity (in seconds).
   * @param <T> Entity type.
   * @return an {@link Observable} eventually containing the found {@link JsonDocument}.
   */
  <T> Observable<T> getAndTouch(Class<T> entityClass, Object id, int expiry);

  /**
   * Check whether a entity with the given ID does exist in the bucket.
   *
   * @param entityClass Entity class.
   * @param id Entity id.
   * @return True, if exists.
   */
  Observable<Boolean> exists(Class<?> entityClass, Object id);

  /**
   * @return A new upsert command.
   */
  AsyncCommand upsert();

  /**
   * Insert or overwrite an entity. If the entity already has an ID, then that ID is selected. If
   * the entity doesn't have an ID and the field is annotated with {@link GeneratedValue} this
   * method will generate a new ID and insert the entity.
   *
   * @param entity Entity to insert or overwrite.
   * @param <T> Entity type.
   * @return Updated entity.
   */
  default <T> Observable<T> upsert(final T entity) {
    return upsert().execute(entity);
  }

  /**
   * @return A new insert command.
   */
  AsyncCommand insert();

  /**
   * Insert an entity. If the entity already has an ID, then that ID is selected. If
   * the entity doesn't have an ID and the field is annotated with {@link GeneratedValue} this
   * method will generate a new ID and insert the entity.
   *
   * @param entity Entity to insert.
   * @param <T> Entity type.
   * @return Updated entity.
   */
  default <T> Observable<T> insert(final T entity) {
    return insert().execute(entity);
  }

  /**
   * Replace an entity if it does exist and watch for durability constraints.
   *
   * @return A new replace command.
   */
  AsyncCommand replace();

  /**
   * Replace an entity if it does exist and watch for durability constraints.
   *
   * This method works exactly like {@link AsyncBucket#replace(Document)}, but afterwards watches
   * the server states if the given durability constraints are met. If this is the case, a new
   * document is returned which contains the original properties, but has the refreshed CAS value
   * set.
   *
   * @param entity An entity to replace.
   * @param <T> Entity type.
   * @return The replace entity.
   */
  default <T> Observable<T> replace(final T entity) {
    return replace().execute(entity);
  }

  /**
   * Removes an entity from the Server.
   *
   * The an entity returned just has the document ID and its CAS value set, since the value and all
   * other associated properties have been removed from the server.
   *
   * @return A new remove command.
   */
  AsyncRemoveCommand remove();

  /**
   * Removes an entity from the Server.
   *
   * The an entity returned just has the document ID and its CAS value set, since the value and all
   * other associated properties have been removed from the server.
   *
   * @param entity Entity to remove.
   * @return The cas value.
   */
  default Observable<Long> remove(final Object entity) {
    return remove().execute(entity);
  }

  /**
   * Removes an entity from the Server.
   *
   * The an entity returned just has the document ID and its CAS value set, since the value and all
   * other associated properties have been removed from the server.
   *
   * @param entityClass Entity class to remove.
   * @param id Entity id.
   * @return The cas value.
   */
  default Observable<Long> remove(final Class<?> entityClass, final Object id) {
    return remove().execute(entityClass, id);
  }

  /**
   * Run a {@link N1qlQuery#simple(Statement)} query.
   *
   * @param statement Statement.
   * @param <T> Entity type.
   * @return A list of results.
   * @see N1Q#from(Class)
   */
  default <T> Observable<List<T>> query(final Statement statement) {
    return query(N1qlQuery.simple(statement));
  }

  /**
   * Run a {@link N1qlQuery} query.
   *
   * @param query Query.
   * @param <T> Entity type.
   * @return A list of results.
   * @see N1Q#from(Class)
   */
  <T> Observable<List<T>> query(N1qlQuery query);

  /**
   * Run a {@link ViewQuery} query.
   *
   * @param query View query.
   * @param <T> Entity type.
   * @return Results.
   */
  <T> Observable<AsyncViewQueryResult<T>> query(ViewQuery query);

}
