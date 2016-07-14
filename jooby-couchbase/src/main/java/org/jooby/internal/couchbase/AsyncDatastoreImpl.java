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
package org.jooby.internal.couchbase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.jooby.couchbase.AsyncDatastore;
import org.jooby.couchbase.N1Q;

import com.couchbase.client.core.message.kv.MutationToken;
import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.PersistTo;
import com.couchbase.client.java.ReplicaMode;
import com.couchbase.client.java.ReplicateTo;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.EntityDocument;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.QueryExecutionException;
import com.couchbase.client.java.query.AsyncN1qlQueryRow;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.repository.AsyncRepository;
import com.couchbase.client.java.view.ViewQuery;

import javaslang.control.Try;
import rx.Observable;
import rx.functions.Func1;

@SuppressWarnings({"rawtypes", "unchecked" })
public class AsyncDatastoreImpl implements AsyncDatastore {

  private static abstract class BaseCommandImpl implements AsyncCommand {

    protected long cas;

    protected int expiry;

    protected MutationToken mutationToken;

    protected Function<Object, Object> idGen;

    public BaseCommandImpl(final Function<Object, Object> idGen) {
      this.idGen = idGen;
    }

    @Override
    public AsyncCommand cas(final long cas) {
      this.cas = cas;
      return this;
    }

    @Override
    public AsyncCommand expiry(final int seconds) {
      this.expiry = seconds;
      return this;
    }

    @Override
    public AsyncCommand mutationToken(final MutationToken mutationToken) {
      this.mutationToken = mutationToken;
      return this;
    }

  }

  private static abstract class CommandImpl extends BaseCommandImpl {

    public CommandImpl(final Function<Object, Object> idGen) {
      super(idGen);
    }

    @Override
    public <R> Observable<R> execute(final Object entity, final PersistTo persistTo,
        final ReplicateTo replicateTo) {
      Object id = idGen.apply(entity);
      String qId = N1Q.qualifyId(entity.getClass(), id);
      EntityDocument<Object> doc = EntityDocument.create(qId, expiry, entity, cas, mutationToken);
      return execute(doc, persistTo, replicateTo);
    }

    public abstract <T> Observable<T> execute(EntityDocument<Object> entity, PersistTo persistTo,
        ReplicateTo replicateTo);

  }

  private static abstract class RemoveCommandImpl extends BaseCommandImpl
      implements AsyncRemoveCommand {

    public RemoveCommandImpl(final Function<Object, Object> idGen) {
      super(idGen);
    }

    @Override
    public Observable<Long> execute(final Object entity, final PersistTo persistTo,
        final ReplicateTo replicateTo) {
      return execute(entity.getClass(), idGen.apply(entity), persistTo, replicateTo);
    }

  }

  private static final Func1 CONTENT = e -> ((EntityDocument) e).content();

  private static final Func1 CAS = e -> ((Document) e).cas();

  private AsyncBucket bucket;

  private AsyncRepository repo;

  private JacksonMapper converter;

  private Function<Object, Object> idGen;

  public AsyncDatastoreImpl(final AsyncBucket bucket, final AsyncRepository repo,
      final Function<Object, Object> idGen, final JacksonMapper converter) {
    this.bucket = bucket;
    this.repo = repo;
    this.idGen = idGen;
    this.converter = converter;
  }

  @Override
  public <T> Observable<T> get(final Class<T> entityClass, final Object id) {
    return repo.get(N1Q.qualifyId(entityClass, id), entityClass)
        .map(CONTENT);
  }

  @Override
  public <T> Observable<T> getFromReplica(final Class<T> entityClass, final Object id,
      final ReplicaMode type) {
    return repo.getFromReplica(N1Q.qualifyId(entityClass, id), type, entityClass)
        .map(CONTENT);
  }

  @Override
  public <T> Observable<T> getAndLock(final Class<T> entityClass, final Object id,
      final int lockTime) {
    return repo.getAndLock(N1Q.qualifyId(entityClass, id), lockTime, entityClass)
        .map(CONTENT);
  }

  @Override
  public <T> Observable<T> getAndTouch(final Class<T> entityClass, final Object id,
      final int expiry) {
    return repo.getAndTouch(N1Q.qualifyId(entityClass, id), expiry, entityClass)
        .map(CONTENT);
  }

  @Override
  public Observable<Boolean> exists(final Class<?> entityClass, final Object id) {
    return repo.exists(N1Q.qualifyId(entityClass, id));
  }

  @Override
  public AsyncCommand upsert() {
    return new CommandImpl(idGen) {
      @Override
      public <T> Observable<T> execute(final EntityDocument<Object> doc, final PersistTo persistTo,
          final ReplicateTo replicateTo) {
        return repo
            .upsert(doc, persistTo, replicateTo)
            .map(CONTENT);
      }
    };
  }

  @Override
  public AsyncCommand insert() {
    return new CommandImpl(idGen) {
      @Override
      public <T> Observable<T> execute(final EntityDocument<Object> doc, final PersistTo persistTo,
          final ReplicateTo replicateTo) {
        return repo
            .insert(doc, persistTo, replicateTo)
            .map(CONTENT);
      }
    };
  }

  @Override
  public AsyncCommand replace() {
    return new CommandImpl(IdGenerator::getId) {
      @Override
      public <T> Observable<T> execute(final EntityDocument<Object> doc, final PersistTo persistTo,
          final ReplicateTo replicateTo) {
        return repo
            .replace(doc, persistTo, replicateTo)
            .map(CONTENT);
      }
    };
  }

  @Override
  public AsyncRemoveCommand remove() {
    return new RemoveCommandImpl(IdGenerator::getId) {
      @Override
      public Observable<Long> execute(final Class<?> entityClass, final Object id,
          final PersistTo persistTo,
          final ReplicateTo replicateTo) {
        String qId = N1Q.qualifyId(entityClass, id);
        JsonDocument doc = JsonDocument.create(qId, null, cas);
        return bucket.remove(doc, persistTo, replicateTo).map(CAS);
      }
    };
  }

  @Override
  public <T> Observable<List<T>> query(final N1qlQuery query) {
    return bucket.query(query)
        .flatMap(aqr -> {
          return Observable.zip(aqr.rows().toList(),
              aqr.errors().toList(),
              aqr.finalSuccess().singleOrDefault(Boolean.FALSE),
              (rows, errors, finalSuccess) -> {
                if (!finalSuccess) {
                  throw new QueryExecutionException(
                      "execution of query resulted in exception: ",
                      Try.of(() -> errors.get(0)).getOrElse((JsonObject) null));
                }
                List<T> value = new ArrayList<>();
                for (AsyncN1qlQueryRow row : rows) {
                  try {
                    T v = converter.fromBytes(row.byteValue());
                    value.add(v);
                  } catch (IOException ex) {
                    throw new QueryExecutionException(
                        "execution of query resulted in exception", null, ex);
                  }
                }
                return value;
              });
        });
  }

  @Override
  public <T> Observable<AsyncViewQueryResult<T>> query(final ViewQuery query) {
    return bucket.query(query)
        .map(result -> {
          Observable<List<T>> rows = result.rows()
              .flatMap(r -> r.document()
                  .map(doc -> {
                    EntityDocument<T> entity = converter.toEntity(doc, null);
                    return entity.content();
                  }))
              .toList();
          return new AsyncViewQueryResult(result.totalRows(), rows);
        });
  }

}
