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

import org.jooby.couchbase.AsyncDatastore.AsyncCommand;
import org.jooby.couchbase.AsyncDatastore.AsyncRemoveCommand;

import com.couchbase.client.core.message.kv.MutationToken;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.PersistTo;
import com.couchbase.client.java.ReplicaMode;
import com.couchbase.client.java.ReplicateTo;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.error.TemporaryLockFailureException;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.repository.Repository;
import com.couchbase.client.java.repository.annotation.Id;
import com.couchbase.client.java.view.ViewQuery;

import rx.Observable;

/**
 * <h1>datastore</h1>
 * <p>
 * Create, read, update and delete entities from an {@link Bucket}. It is similar to
 * {@link Repository} but less verbose or more ready to use, but also add support for
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
public interface Datastore {

  /**
   * Result of {@link Datastore#query(ViewQuery)} contains a list of entities plus totalRows in
   * the view.
   *
   * @author edgar
   * @param <T> Entity type.
   */
  class ViewQueryResult<T> {
    /** Total number of rows in the view. */
    private int totalRows;

    /** List of rows from current execution. */
    private List<T> rows;

    /**
     * Creates a new {@link ViewQueryResult}.
     *
     * @param totalRows Total number of rows in the view.
     * @param rows Resultset from current execution.
     */
    ViewQueryResult(final int totalRows, final List<T> rows) {
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
    public List<T> getRows() {
      return rows;
    }

  }

  /**
   * Provides advanced options for couchbase operation.
   *
   * @author edgar
   */
  class Command {
    protected final AsyncCommand cmd;

    Command(final AsyncCommand cmd) {
      this.cmd = cmd;
    }

    /**
     * Set the expiry/ttl entity option.
     *
     * @param expiry The expiration time expressed as relative seconds from now.
     * @return This command.
     */
    public Command expiry(final int expiry) {
      cmd.expiry(expiry);
      return this;
    }

    /**
     * Set a CAS value for the entity (0 if not set).
     *
     * @param cas Cas value.
     * @return This command.
     */
    public Command cas(final long cas) {
      cmd.cas(cas);
      return this;
    }

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
    public Command mutationToken(final MutationToken mutationToken) {
      cmd.mutationToken(mutationToken);
      return this;
    }

    /**
     * Execute this command.
     *
     * @param entity Entity to use.
     * @param <R> Entity type.
     * @return Command result.
     */
    public <R> R execute(final Object entity) {
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
    public <R> R execute(final Object entity, final PersistTo persistTo) {
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
    public <R> R execute(final Object entity, final ReplicateTo replicateTo) {
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
    public <R> R execute(final Object entity, final PersistTo persistTo,
        final ReplicateTo replicateTo) {
      Observable<R> result = cmd.execute(entity, persistTo, replicateTo);
      return result.toBlocking().single();
    }
  }

  /**
   * Provides advanced options for couchbase operation.
   *
   * @author edgar
   */
  class RemoveCommand extends Command {

    /**
     * Creates a new remove command.
     *
     * @param cmd Delegate to async version.
     */
    RemoveCommand(final AsyncRemoveCommand cmd) {
      super(cmd);
    }

    /**
     * Execute a remove document operation
     *
     * @param entity Entity to remove.
     * @param persistTo Persist to option.
     * @param replicateTo Replicate to option.
     * @return CAS value.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Long execute(final Object entity, final PersistTo persistTo,
        final ReplicateTo replicateTo) {
      Observable<Long> result = cmd.execute(entity, persistTo, replicateTo);
      return result.toBlocking().single();
    }

    /**
     * Execute a remove document operation
     *
     * @param entityClass Entity class to remove.
     * @param id Entity id to remove.
     * @return CAS value.
     */
    public Long execute(final Class<?> entityClass, final Object id) {
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
    public Long execute(final Class<?> entityClass, final Object id,
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
    public Long execute(final Class<?> entityClass, final Object id,
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
    public Long execute(final Class<?> entityClass, final Object id, final PersistTo persistTo,
        final ReplicateTo replicateTo) {
      return ((AsyncRemoveCommand) cmd).execute(entityClass, id, persistTo, replicateTo)
          .toBlocking().single();
    }
  }

  /**
   * Produces an observable that throws a {@link DocumentDoesNotExistException}.
   *
   * @param entityClass Entity class.
   * @param id Entity id.
   * @param <T> Entity type.
   * @return An observable that throws a {@link DocumentDoesNotExistException}.
   */
  @SuppressWarnings("rawtypes")
  static <T> Observable<T> notFound(final Class entityClass, final Object id) {
    return Observable.create(s -> {
      s.onError(new DocumentDoesNotExistException(N1Q.qualifyId(entityClass, id)));
    });
  }

  /**
   * @return An {@link AsyncDatastore}.
   */
  AsyncDatastore async();

  /**
   * Get an entity/document by ID. The unique ID is constructed via
   * {@link N1Q#qualifyId(Class, Object)}.
   *
   * If the entity is found, a entity is returned. Otherwise a
   * {@link DocumentDoesNotExistException}.
   *
   * @param entityClass Entity class.
   * @param id Entity id.
   * @param <T> Entity type.
   * @return An entity matching the id or an empty observable.
   */
  default <T> T get(final Class<T> entityClass, final Object id)
      throws DocumentDoesNotExistException {
    return async().<T> get(entityClass, id)
        .switchIfEmpty(notFound(entityClass, id))
        .toBlocking().single();
  }

  /**
   * Get an entity/document by ID. The unique ID is constructed via
   * {@link N1Q#qualifyId(Class, Object)}.
   *
   * If the entity is found, a entity is returned. Otherwise a
   * {@link DocumentDoesNotExistException}.
   *
   * @param entityClass Entity class.
   * @param id Entity id.
   * @param mode Replica mode.
   * @param <T> Entity type.
   * @return An entity matching the id or an empty observable.
   */
  default <T> T getFromReplica(final Class<T> entityClass, final Object id,
      final ReplicaMode mode) throws DocumentDoesNotExistException {
    return async().<T> getFromReplica(entityClass, id, mode)
        .switchIfEmpty(notFound(entityClass, id))
        .toBlocking().single();
  }

  /**
   * Retrieve and lock a entity by its unique ID.
   *
   * If the entity is found, a entity is returned. Otherwise a
   * {@link DocumentDoesNotExistException}.
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
  default <T> T getAndLock(final Class<T> entityClass, final Object id, final int lockTime)
      throws DocumentDoesNotExistException {
    return async().<T> getAndLock(entityClass, id, lockTime)
        .switchIfEmpty(notFound(entityClass, id))
        .toBlocking().single();
  }

  /**
   * Retrieve and touch an entity by its unique ID.
   *
   * If the entity is found, an entity is returned. Otherwise a
   * {@link DocumentDoesNotExistException}.
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
  default <T> T getAndTouch(final Class<T> entityClass, final Object id, final int expiry)
      throws DocumentDoesNotExistException {
    return async().<T> getAndTouch(entityClass, id, expiry)
        .switchIfEmpty(notFound(entityClass, id))
        .toBlocking().single();
  }

  /**
   * Check whether a entity with the given ID does exist in the bucket.
   *
   * @param entityClass Entity class.
   * @param id Entity id.
   * @return True, if exists.
   */
  default boolean exists(final Class<?> entityClass, final Object id) {
    return async().exists(entityClass, id).toBlocking().single();
  }

  /**
   * @return A new upsert command.
   */
  default Command upsert() {
    return new Command(async().upsert());
  }

  /**
   * Insert or overwrite an entity. If the entity already has an ID, then that ID is selected. If
   * the entity doesn't have an ID and the field is annotated with {@link GeneratedValue} this
   * method will generate a new ID and insert the entity.
   *
   * @param entity Entity to insert or overwrite.
   * @param <T> Entity type.
   * @return Updated entity.
   */
  default <T> T upsert(final T entity) {
    return upsert().execute(entity);
  }

  /**
   * @return A new insert command.
   */
  default Command insert() {
    return new Command(async().insert());
  }

  /**
   * Insert an entity. If the entity already has an ID, then that ID is selected. If
   * the entity doesn't have an ID and the field is annotated with {@link GeneratedValue} this
   * method will generate a new ID and insert the entity.
   *
   * @param entity Entity to insert or overwrite.
   * @param <T> Entity type.
   * @return Updated entity.
   */
  default <T> T insert(final T entity) {
    return insert().execute(entity);
  }

  /**
   * @return A new replace command.
   */
  default Command replace() {
    return new Command(async().replace());
  }

  /**
   * Replace a {@link Document} if it does exist and watch for durability constraints.
   *
   * It watches the server states if the given durability constraints are met. If this is the case,
   * a new document is returned which contains the original properties, but has the refreshed CAS
   * value set.
   *
   * @param entity Entity to replace.
   * @param <T> Entity type.
   * @return A new replace command.
   */
  default <T> T replace(final T entity) {
    return replace().execute(entity);
  }

  /**
   * Removes an entity from the Server.
   *
   * The an entity returned just has the document ID and its CAS value set, since the value and all
   * other
   * associated properties have been removed from the server.
   *
   * @return A new remove command.
   */
  default RemoveCommand remove() {
    return new RemoveCommand(async().remove());
  }

  /**
   * Removes an entity from the Server.
   *
   * The an entity returned just has the document ID and its CAS value set, since the value and all
   * other associated properties have been removed from the server.
   *
   * Throws a {@link DocumentDoesNotExistException} if entity doesn't exist.
   *
   * @param entity Entity to remove.
   * @return The cas value.
   */
  default long remove(final Object entity) throws DocumentDoesNotExistException {
    return remove().execute(entity);
  }

  /**
   * Removes an entity from the Server.
   *
   * The an entity returned just has the document ID and its CAS value set, since the value and all
   * other associated properties have been removed from the server.
   *
   * Throws a {@link DocumentDoesNotExistException} if entity doesn't exist.
   *
   * @param entityClass Entity class to remove.
   * @param id Entity id.
   * @return The cas value.
   */
  default long remove(final Class<?> entityClass, final Object id)
      throws DocumentDoesNotExistException {
    return remove().execute(entityClass, id);
  }

  /**
   * Run a {@link N1qlQuery#simple(Statement)} query.
   *
   * @param query N1qlQuery.
   * @param <T> Entity type.
   * @return A list of results.
   * @see N1Q#from(Class)
   */
  default <T> List<T> query(final N1qlQuery query) {
    return async().<T> query(query).toBlocking().single();
  }

  /**
   * Run a {@link N1qlQuery#simple(Statement)} query.
   *
   * @param statement Statement.
   * @param <T> Entity type.
   * @return A list of results.
   * @see N1Q#from(Class)
   */
  default <T> List<T> query(final Statement statement) {
    return async().<T> query(statement).toBlocking().single();
  }

  /**
   * Run a {@link ViewQuery} query.
   *
   * @param query View query.
   * @param <T> Entity type.
   * @return Results.
   */
  default <T> ViewQueryResult<T> query(final ViewQuery query) {
    return async().<T> query(query)
        .map(r -> new ViewQueryResult<>(r.getTotalRows(), r.getRows().toBlocking().single()))
        .toBlocking().single();
  }
}
