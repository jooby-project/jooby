package org.jooby.internal.couchbase;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

import org.jooby.couchbase.AsyncDatastore.AsyncViewQueryResult;
import org.jooby.couchbase.N1Q;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;

import com.couchbase.client.core.message.kv.MutationToken;
import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.PersistTo;
import com.couchbase.client.java.ReplicaMode;
import com.couchbase.client.java.ReplicateTo;
import com.couchbase.client.java.document.EntityDocument;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.QueryExecutionException;
import com.couchbase.client.java.query.AsyncN1qlQueryResult;
import com.couchbase.client.java.query.AsyncN1qlQueryRow;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.repository.AsyncRepository;
import com.couchbase.client.java.view.AsyncViewResult;
import com.couchbase.client.java.view.AsyncViewRow;
import com.couchbase.client.java.view.ViewQuery;

import rx.Observable;

public class AsyncDatastoreImplTest {

  public static class Entity {

    public Long id;

    public Entity(final long l) {
      this.id = l;
    }

    public Entity() {
    }

    @Override
    public String toString() {
      return id.toString();
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void getById() throws Exception {
    Class entityClass = Entity.class;
    Long id = 7L;
    new MockUnit(AsyncBucket.class, AsyncRepository.class, Function.class, JacksonMapper.class)
        .expect(unit -> {
          AsyncRepository repo = unit.get(AsyncRepository.class);
          expect(repo.get(N1Q.qualifyId(entityClass, id), entityClass))
              .andReturn(document(new Entity(id)));
        })
        .run(unit -> {
          Entity e = (Entity) new AsyncDatastoreImpl(unit.get(AsyncBucket.class),
              unit.get(AsyncRepository.class),
              unit.get(Function.class), unit.get(JacksonMapper.class))
                  .get(entityClass, id)
                  .toBlocking()
                  .single();
          assertNotNull(e);
          assertEquals(id, e.id);
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void getFromReplica() throws Exception {
    Class entityClass = Entity.class;
    Long id = 7L;
    new MockUnit(AsyncBucket.class, AsyncRepository.class, Function.class, JacksonMapper.class)
        .expect(unit -> {
          AsyncRepository repo = unit.get(AsyncRepository.class);
          expect(
              repo.getFromReplica(N1Q.qualifyId(entityClass, id), ReplicaMode.FIRST, entityClass))
                  .andReturn(document(new Entity(id)));
        })
        .run(unit -> {
          Entity e = (Entity) new AsyncDatastoreImpl(unit.get(AsyncBucket.class),
              unit.get(AsyncRepository.class),
              unit.get(Function.class), unit.get(JacksonMapper.class))
                  .getFromReplica(entityClass, id, ReplicaMode.FIRST)
                  .toBlocking()
                  .single();
          assertNotNull(e);
          assertEquals(id, e.id);
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void getAndTouch() throws Exception {
    Class entityClass = Entity.class;
    Long id = 7L;
    new MockUnit(AsyncBucket.class, AsyncRepository.class, Function.class, JacksonMapper.class)
        .expect(unit -> {
          AsyncRepository repo = unit.get(AsyncRepository.class);
          expect(repo.getAndTouch(N1Q.qualifyId(entityClass, id), 5, entityClass))
              .andReturn(document(new Entity(id)));
        })
        .run(unit -> {
          Entity e = (Entity) new AsyncDatastoreImpl(unit.get(AsyncBucket.class),
              unit.get(AsyncRepository.class),
              unit.get(Function.class), unit.get(JacksonMapper.class))
                  .getAndTouch(entityClass, id, 5)
                  .toBlocking()
                  .single();
          assertNotNull(e);
          assertEquals(id, e.id);
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void getAndLock() throws Exception {
    Class entityClass = Entity.class;
    Long id = 7L;
    new MockUnit(AsyncBucket.class, AsyncRepository.class, Function.class, JacksonMapper.class)
        .expect(unit -> {
          AsyncRepository repo = unit.get(AsyncRepository.class);
          expect(repo.getAndLock(N1Q.qualifyId(entityClass, id), 5, entityClass))
              .andReturn(document(new Entity(id)));
        })
        .run(unit -> {
          Entity e = (Entity) new AsyncDatastoreImpl(unit.get(AsyncBucket.class),
              unit.get(AsyncRepository.class),
              unit.get(Function.class), unit.get(JacksonMapper.class))
                  .getAndLock(entityClass, id, 5)
                  .toBlocking()
                  .single();
          assertNotNull(e);
          assertEquals(id, e.id);
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void exists() throws Exception {
    Class entityClass = Entity.class;
    Long id = 7L;
    new MockUnit(AsyncBucket.class, AsyncRepository.class, Function.class, JacksonMapper.class)
        .expect(unit -> {
          AsyncRepository repo = unit.get(AsyncRepository.class);
          expect(repo.exists(N1Q.qualifyId(entityClass, id)))
              .andReturn(Observable.just(true));
        })
        .run(unit -> {
          boolean exists = new AsyncDatastoreImpl(unit.get(AsyncBucket.class),
              unit.get(AsyncRepository.class),
              unit.get(Function.class), unit.get(JacksonMapper.class))
                  .exists(entityClass, id)
                  .toBlocking()
                  .single();
          assertEquals(true, exists);
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void upsert() throws Exception {
    Long id = 7L;
    Entity entity = new Entity(id);
    new MockUnit(AsyncBucket.class, AsyncRepository.class, Function.class, JacksonMapper.class)
        .expect(idGen(entity, id))
        .expect(unit -> {
          AsyncRepository repo = unit.get(AsyncRepository.class);
          EntityDocument doc = unit.capture(EntityDocument.class);
          expect(repo.upsert(doc, eq(PersistTo.NONE), eq(ReplicateTo.NONE)))
              .andReturn(document(entity));
        })
        .run(unit -> {
          Entity r = new AsyncDatastoreImpl(unit.get(AsyncBucket.class),
              unit.get(AsyncRepository.class),
              unit.get(Function.class), unit.get(JacksonMapper.class))
                  .upsert(entity)
                  .toBlocking()
                  .single();
          assertEquals(entity, r);
        }, unit -> {
          EntityDocument doc = unit.captured(EntityDocument.class).iterator().next();
          assertEquals(entity, doc.content());
        });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void upsertWithCustomReplicate() throws Exception {
    Long id = 7L;
    Entity entity = new Entity(id);
    new MockUnit(AsyncBucket.class, AsyncRepository.class, Function.class, JacksonMapper.class)
        .expect(idGen(entity, id))
        .expect(unit -> {
          AsyncRepository repo = unit.get(AsyncRepository.class);
          EntityDocument doc = unit.capture(EntityDocument.class);
          expect(repo.upsert(doc, eq(PersistTo.NONE), eq(ReplicateTo.ONE)))
              .andReturn(document(entity));
        })
        .run(unit -> {
          Observable<Object> r = new AsyncDatastoreImpl(unit.get(AsyncBucket.class),
              unit.get(AsyncRepository.class),
              unit.get(Function.class), unit.get(JacksonMapper.class))
                  .upsert()
                  .execute(entity, ReplicateTo.ONE);
          assertEquals(entity, r.toBlocking().single());
        }, unit -> {
          EntityDocument doc = unit.captured(EntityDocument.class).iterator().next();
          assertEquals(entity, doc.content());
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void insert() throws Exception {
    Long id = 7L;
    Entity entity = new Entity(id);
    new MockUnit(AsyncBucket.class, AsyncRepository.class, Function.class, JacksonMapper.class)
        .expect(idGen(entity, id))
        .expect(unit -> {
          AsyncRepository repo = unit.get(AsyncRepository.class);
          EntityDocument doc = unit.capture(EntityDocument.class);
          expect(repo.insert(doc, eq(PersistTo.NONE), eq(ReplicateTo.NONE)))
              .andReturn(document(entity));
        })
        .run(unit -> {
          Entity r = new AsyncDatastoreImpl(unit.get(AsyncBucket.class),
              unit.get(AsyncRepository.class),
              unit.get(Function.class), unit.get(JacksonMapper.class))
                  .insert(entity)
                  .toBlocking()
                  .single();
          assertEquals(entity, r);
        }, unit -> {
          EntityDocument doc = unit.captured(EntityDocument.class).iterator().next();
          assertEquals(entity, doc.content());
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void replace() throws Exception {
    Long id = 7L;
    Entity entity = new Entity(id);
    new MockUnit(AsyncBucket.class, AsyncRepository.class, Function.class, JacksonMapper.class)
        .expect(unit -> {
          AsyncRepository repo = unit.get(AsyncRepository.class);
          EntityDocument doc = unit.capture(EntityDocument.class);
          expect(repo.replace(doc, eq(PersistTo.NONE), eq(ReplicateTo.NONE)))
              .andReturn(document(entity));
        })
        .run(unit -> {
          Entity r = new AsyncDatastoreImpl(unit.get(AsyncBucket.class),
              unit.get(AsyncRepository.class),
              unit.get(Function.class), unit.get(JacksonMapper.class))
                  .replace(entity)
                  .toBlocking()
                  .single();
          assertEquals(entity, r);
        }, unit -> {
          EntityDocument doc = unit.captured(EntityDocument.class).iterator().next();
          assertEquals(entity, doc.content());
        });
  }

  @SuppressWarnings({"unchecked" })
  @Test
  public void removeEntity() throws Exception {
    Long id = 7L;
    Entity entity = new Entity(id);
    new MockUnit(AsyncBucket.class, AsyncRepository.class, Function.class, JacksonMapper.class)
        .expect(unit -> {
          AsyncBucket bucket = unit.get(AsyncBucket.class);
          JsonDocument doc = unit.capture(JsonDocument.class);
          JsonDocument cas = JsonDocument.create(id.toString(), null, 1);
          expect(bucket.remove(doc, eq(PersistTo.NONE), eq(ReplicateTo.NONE)))
              .andReturn(Observable.just(cas));
        })
        .run(unit -> {
          long cas = new AsyncDatastoreImpl(unit.get(AsyncBucket.class),
              unit.get(AsyncRepository.class),
              unit.get(Function.class), unit.get(JacksonMapper.class))
                  .remove(entity)
                  .toBlocking()
                  .single();
          assertEquals(1, cas);
        }, unit -> {
          JsonDocument doc = unit.captured(JsonDocument.class).iterator().next();
          assertEquals(N1Q.qualifyId(entity.getClass(), id), doc.id());
        });
  }

  @SuppressWarnings({"unchecked" })
  @Test
  public void removeEntityById() throws Exception {
    Long id = 7L;
    Entity entity = new Entity(id);
    new MockUnit(AsyncBucket.class, AsyncRepository.class, Function.class, JacksonMapper.class)
        .expect(unit -> {
          AsyncBucket bucket = unit.get(AsyncBucket.class);
          JsonDocument doc = unit.capture(JsonDocument.class);
          JsonDocument cas = JsonDocument.create(id.toString(), null, 1);
          expect(bucket.remove(doc, eq(PersistTo.NONE), eq(ReplicateTo.NONE)))
              .andReturn(Observable.just(cas));
        })
        .run(unit -> {
          long cas = new AsyncDatastoreImpl(unit.get(AsyncBucket.class),
              unit.get(AsyncRepository.class),
              unit.get(Function.class), unit.get(JacksonMapper.class))
                  .remove(entity.getClass(), id)
                  .toBlocking()
                  .single();
          assertEquals(1L, cas);
        }, unit -> {
          JsonDocument doc = unit.captured(JsonDocument.class).iterator().next();
          assertEquals(N1Q.qualifyId(entity.getClass(), id), doc.id());
        });
  }

  @SuppressWarnings({"unchecked" })
  @Test
  public void removeEntityByIdWithReplicate() throws Exception {
    Long id = 7L;
    Entity entity = new Entity(id);
    new MockUnit(AsyncBucket.class, AsyncRepository.class, Function.class, JacksonMapper.class)
        .expect(unit -> {
          AsyncBucket bucket = unit.get(AsyncBucket.class);
          JsonDocument doc = unit.capture(JsonDocument.class);
          JsonDocument cas = JsonDocument.create(id.toString(), null, 1);
          expect(bucket.remove(doc, eq(PersistTo.NONE), eq(ReplicateTo.NONE)))
              .andReturn(Observable.just(cas));
        })
        .run(unit -> {
          long cas = new AsyncDatastoreImpl(unit.get(AsyncBucket.class),
              unit.get(AsyncRepository.class),
              unit.get(Function.class), unit.get(JacksonMapper.class))
                  .remove()
                  .execute(entity.getClass(), id, ReplicateTo.NONE)
                  .toBlocking()
                  .single();
          assertEquals(1L, cas);
        }, unit -> {
          JsonDocument doc = unit.captured(JsonDocument.class).iterator().next();
          assertEquals(N1Q.qualifyId(entity.getClass(), id), doc.id());
        });
  }

  @SuppressWarnings({"unchecked" })
  @Test
  public void removeEntityWithCas() throws Exception {
    Long id = 7L;
    Entity entity = new Entity(id);
    new MockUnit(AsyncBucket.class, AsyncRepository.class, Function.class, JacksonMapper.class)
        .expect(unit -> {
          AsyncBucket bucket = unit.get(AsyncBucket.class);
          JsonDocument doc = unit.capture(JsonDocument.class);
          JsonDocument cas = JsonDocument.create(id.toString(), null, 1);
          expect(bucket.remove(doc, eq(PersistTo.NONE), eq(ReplicateTo.NONE)))
              .andReturn(Observable.just(cas));
        })
        .run(unit -> {
          Observable<Long> cas = new AsyncDatastoreImpl(unit.get(AsyncBucket.class),
              unit.get(AsyncRepository.class),
              unit.get(Function.class), unit.get(JacksonMapper.class))
                  .remove()
                  .cas(10L)
                  .execute(entity);
          assertEquals(1L, cas.toBlocking().single().longValue());
        }, unit -> {
          JsonDocument doc = unit.captured(JsonDocument.class).iterator().next();
          assertEquals(N1Q.qualifyId(entity.getClass(), id), doc.id());
          assertEquals(10L, doc.cas());
        });
  }

  @SuppressWarnings({"unchecked" })
  @Test
  public void removeEntityWithReplicate() throws Exception {
    Long id = 7L;
    Entity entity = new Entity(id);
    new MockUnit(AsyncBucket.class, AsyncRepository.class, Function.class, JacksonMapper.class)
        .expect(unit -> {
          AsyncBucket bucket = unit.get(AsyncBucket.class);
          JsonDocument doc = unit.capture(JsonDocument.class);
          JsonDocument cas = JsonDocument.create(id.toString(), null, 1);
          expect(bucket.remove(doc, eq(PersistTo.NONE), eq(ReplicateTo.ONE)))
              .andReturn(Observable.just(cas));
        })
        .run(unit -> {
          Observable<Long> cas = new AsyncDatastoreImpl(unit.get(AsyncBucket.class),
              unit.get(AsyncRepository.class),
              unit.get(Function.class), unit.get(JacksonMapper.class))
                  .remove()
                  .cas(10L)
                  .execute(entity, ReplicateTo.ONE);
          assertEquals(1L, cas.toBlocking().single().longValue());
        }, unit -> {
          JsonDocument doc = unit.captured(JsonDocument.class).iterator().next();
          assertEquals(N1Q.qualifyId(entity.getClass(), id), doc.id());
          assertEquals(10L, doc.cas());
        });
  }

  @SuppressWarnings({"unchecked" })
  @Test
  public void query() throws Exception {
    byte[] bytes = ("{\"class\": \"" + Entity.class.getName()
        + "\",id: 1}").getBytes(StandardCharsets.UTF_8);
    Entity entity = new Entity(1);
    new MockUnit(AsyncBucket.class, AsyncRepository.class, Function.class, JacksonMapper.class,
        N1qlQuery.class, AsyncN1qlQueryResult.class, AsyncN1qlQueryRow.class)
            .expect(unit -> {
              JacksonMapper mapper = unit.get(JacksonMapper.class);
              expect(mapper.fromBytes(bytes)).andReturn(entity);
            })
            .expect(unit -> {
              AsyncN1qlQueryRow row = unit.get(AsyncN1qlQueryRow.class);
              expect(row.byteValue()).andReturn(bytes);

              AsyncN1qlQueryResult result = unit.get(AsyncN1qlQueryResult.class);
              expect(result.rows()).andReturn(Observable.just(row));
              expect(result.errors()).andReturn(Observable.empty());
              expect(result.finalSuccess()).andReturn(Observable.just(true));

              AsyncBucket bucket = unit.get(AsyncBucket.class);
              expect(bucket.query(unit.get(N1qlQuery.class))).andReturn(Observable.just(result));
            })
            .run(unit -> {
              List<Entity> entities = new AsyncDatastoreImpl(unit.get(AsyncBucket.class),
                  unit.get(AsyncRepository.class),
                  unit.get(Function.class), unit.get(JacksonMapper.class))
                      .<Entity> query(unit.get(N1qlQuery.class))
                      .toBlocking()
                      .single();
              assertNotNull(entities);
              assertEquals(entity, entities.get(0));
            });
  }

  @SuppressWarnings({"unchecked" })
  @Test
  public void queryStmt() throws Exception {
    byte[] bytes = ("{\"class\": \"" + Entity.class.getName()
        + "\",id: 1}").getBytes(StandardCharsets.UTF_8);
    Entity entity = new Entity(1);
    new MockUnit(AsyncBucket.class, AsyncRepository.class, Function.class, JacksonMapper.class,
        Statement.class, AsyncN1qlQueryResult.class, AsyncN1qlQueryRow.class)
            .expect(unit -> {
              JacksonMapper mapper = unit.get(JacksonMapper.class);
              expect(mapper.fromBytes(bytes)).andReturn(entity);
            })
            .expect(unit -> {
              AsyncN1qlQueryRow row = unit.get(AsyncN1qlQueryRow.class);
              expect(row.byteValue()).andReturn(bytes);

              AsyncN1qlQueryResult result = unit.get(AsyncN1qlQueryResult.class);
              expect(result.rows()).andReturn(Observable.just(row));
              expect(result.errors()).andReturn(Observable.empty());
              expect(result.finalSuccess()).andReturn(Observable.just(true));

              AsyncBucket bucket = unit.get(AsyncBucket.class);
              expect(bucket.query(unit.capture(N1qlQuery.class)))
                  .andReturn(Observable.just(result));
            })
            .run(unit -> {
              List<Entity> entities = new AsyncDatastoreImpl(unit.get(AsyncBucket.class),
                  unit.get(AsyncRepository.class),
                  unit.get(Function.class), unit.get(JacksonMapper.class))
                      .<Entity> query(unit.get(Statement.class))
                      .toBlocking()
                      .single();
              assertNotNull(entities);
              assertEquals(entity, entities.get(0));
            });
  }

  @SuppressWarnings({"unchecked" })
  @Test
  public void viewQuery() throws Exception {
    ("{\"class\": \"" + Entity.class.getName()
        + "\",id: 1}").getBytes(StandardCharsets.UTF_8);
    Entity entity = new Entity(1);
    new MockUnit(AsyncBucket.class, AsyncRepository.class, Function.class, JacksonMapper.class,
        ViewQuery.class, AsyncViewResult.class, AsyncViewRow.class, JsonDocument.class)
            .expect(unit -> {
              JacksonMapper mapper = unit.get(JacksonMapper.class);
              expect(mapper.toEntity(unit.get(JsonDocument.class), null))
                  .andReturn(EntityDocument.create(entity));
            })
            .expect(unit -> {
              JsonDocument doc = unit.get(JsonDocument.class);

              AsyncViewRow row = unit.get(AsyncViewRow.class);
              expect(row.document()).andReturn(Observable.just(doc));

              AsyncViewResult result = unit.get(AsyncViewResult.class);
              expect(result.rows()).andReturn(Observable.just(row));
              expect(result.totalRows()).andReturn(10);

              AsyncBucket bucket = unit.get(AsyncBucket.class);
              expect(bucket.query(unit.get(ViewQuery.class))).andReturn(Observable.just(result));
            })
            .run(unit -> {
              AsyncViewQueryResult<Entity> result = new AsyncDatastoreImpl(
                  unit.get(AsyncBucket.class),
                  unit.get(AsyncRepository.class),
                  unit.get(Function.class), unit.get(JacksonMapper.class))
                      .<Entity> query(unit.get(ViewQuery.class))
                      .toBlocking()
                      .single();
              assertNotNull(result);
              List<Entity> rows = result.getRows().toBlocking().single();
              assertNotNull(rows);
              assertEquals(entity, rows.get(0));
              assertEquals(10, result.getTotalRows());
            });
  }

  @SuppressWarnings({"unchecked" })
  @Test(expected = QueryExecutionException.class)
  public void queryMappingFaiulre() throws Exception {
    byte[] bytes = ("{\"class\": \"" + Entity.class.getName()
        + "\",id: 1}").getBytes(StandardCharsets.UTF_8);
    new MockUnit(AsyncBucket.class, AsyncRepository.class, Function.class, JacksonMapper.class,
        N1qlQuery.class, AsyncN1qlQueryResult.class, AsyncN1qlQueryRow.class)
            .expect(unit -> {
              JacksonMapper mapper = unit.get(JacksonMapper.class);
              expect(mapper.fromBytes(bytes)).andThrow(new IOException("intentional err"));
            })
            .expect(unit -> {
              AsyncN1qlQueryRow row = unit.get(AsyncN1qlQueryRow.class);
              expect(row.byteValue()).andReturn(bytes);

              AsyncN1qlQueryResult result = unit.get(AsyncN1qlQueryResult.class);
              expect(result.rows()).andReturn(Observable.just(row));
              expect(result.errors()).andReturn(Observable.empty());
              expect(result.finalSuccess()).andReturn(Observable.just(true));

              AsyncBucket bucket = unit.get(AsyncBucket.class);
              expect(bucket.query(unit.get(N1qlQuery.class))).andReturn(Observable.just(result));
            })
            .run(unit -> {
              new AsyncDatastoreImpl(unit.get(AsyncBucket.class),
                  unit.get(AsyncRepository.class),
                  unit.get(Function.class), unit.get(JacksonMapper.class))
                      .<Entity> query(unit.get(N1qlQuery.class))
                      .toBlocking()
                      .single();
            });
  }

  @SuppressWarnings({"unchecked" })
  @Test(expected = QueryExecutionException.class)
  public void queryFailure() throws Exception {
    new MockUnit(AsyncBucket.class, AsyncRepository.class, Function.class, JacksonMapper.class,
        N1qlQuery.class, AsyncN1qlQueryResult.class, AsyncN1qlQueryRow.class)
            .expect(unit -> {
              AsyncN1qlQueryRow row = unit.get(AsyncN1qlQueryRow.class);

              AsyncN1qlQueryResult result = unit.get(AsyncN1qlQueryResult.class);
              expect(result.rows()).andReturn(Observable.just(row));
              expect(result.errors())
                  .andReturn(Observable.just(JsonObject.create().put("foo", "bar")));
              expect(result.finalSuccess()).andReturn(Observable.just(false));

              AsyncBucket bucket = unit.get(AsyncBucket.class);
              expect(bucket.query(unit.get(N1qlQuery.class))).andReturn(Observable.just(result));
            })
            .run(unit -> {
              new AsyncDatastoreImpl(unit.get(AsyncBucket.class),
                  unit.get(AsyncRepository.class),
                  unit.get(Function.class), unit.get(JacksonMapper.class))
                      .<Entity> query(unit.get(N1qlQuery.class))
                      .toBlocking()
                      .single();
            });
  }

  @SuppressWarnings({"unchecked" })
  @Test(expected = QueryExecutionException.class)
  public void queryFailureNoError() throws Exception {
    new MockUnit(AsyncBucket.class, AsyncRepository.class, Function.class, JacksonMapper.class,
        N1qlQuery.class, AsyncN1qlQueryResult.class, AsyncN1qlQueryRow.class)
            .expect(unit -> {
              AsyncN1qlQueryRow row = unit.get(AsyncN1qlQueryRow.class);

              AsyncN1qlQueryResult result = unit.get(AsyncN1qlQueryResult.class);
              expect(result.rows()).andReturn(Observable.just(row));
              expect(result.errors()).andReturn(Observable.empty());
              expect(result.finalSuccess()).andReturn(Observable.just(false));

              AsyncBucket bucket = unit.get(AsyncBucket.class);
              expect(bucket.query(unit.get(N1qlQuery.class))).andReturn(Observable.just(result));
            })
            .run(unit -> {
              new AsyncDatastoreImpl(unit.get(AsyncBucket.class),
                  unit.get(AsyncRepository.class),
                  unit.get(Function.class), unit.get(JacksonMapper.class))
                      .<Entity> query(unit.get(N1qlQuery.class))
                      .toBlocking()
                      .single();
            });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void upsertCas() throws Exception {
    Long id = 7L;
    long cas = 10L;
    Entity entity = new Entity(id);
    new MockUnit(AsyncBucket.class, AsyncRepository.class, Function.class, JacksonMapper.class)
        .expect(idGen(entity, id))
        .expect(unit -> {
          AsyncRepository repo = unit.get(AsyncRepository.class);
          EntityDocument doc = unit.capture(EntityDocument.class);
          expect(repo.upsert(doc, eq(PersistTo.NONE), eq(ReplicateTo.NONE)))
              .andReturn(document(entity));
        })
        .run(unit -> {
          Entity r = (Entity) new AsyncDatastoreImpl(unit.get(AsyncBucket.class),
              unit.get(AsyncRepository.class),
              unit.get(Function.class), unit.get(JacksonMapper.class))
                  .upsert()
                  .cas(cas)
                  .execute(entity)
                  .toBlocking()
                  .single();
          assertEquals(entity, r);
        }, unit -> {
          EntityDocument doc = unit.captured(EntityDocument.class).iterator().next();
          assertEquals(entity, doc.content());
          assertEquals(cas, doc.cas());
        });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void upsertExpiry() throws Exception {
    Long id = 7L;
    int expiry = 10;
    Entity entity = new Entity(id);
    new MockUnit(AsyncBucket.class, AsyncRepository.class, Function.class, JacksonMapper.class)
        .expect(idGen(entity, id))
        .expect(unit -> {
          AsyncRepository repo = unit.get(AsyncRepository.class);
          EntityDocument doc = unit.capture(EntityDocument.class);
          expect(repo.upsert(doc, eq(PersistTo.NONE), eq(ReplicateTo.NONE)))
              .andReturn(document(entity));
        })
        .run(unit -> {
          Entity r = (Entity) new AsyncDatastoreImpl(unit.get(AsyncBucket.class),
              unit.get(AsyncRepository.class),
              unit.get(Function.class), unit.get(JacksonMapper.class))
                  .upsert()
                  .expiry(expiry)
                  .execute(entity)
                  .toBlocking()
                  .single();
          assertEquals(entity, r);
        }, unit -> {
          EntityDocument doc = unit.captured(EntityDocument.class).iterator().next();
          assertEquals(entity, doc.content());
          assertEquals(expiry, doc.expiry());
        });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void upsertMutToken() throws Exception {
    Long id = 7L;
    MutationToken token = new MutationToken(0, 0, 0, null);
    Entity entity = new Entity(id);
    new MockUnit(AsyncBucket.class, AsyncRepository.class, Function.class, JacksonMapper.class)
        .expect(idGen(entity, id))
        .expect(unit -> {
          AsyncRepository repo = unit.get(AsyncRepository.class);
          EntityDocument doc = unit.capture(EntityDocument.class);
          expect(repo.upsert(doc, eq(PersistTo.NONE), eq(ReplicateTo.NONE)))
              .andReturn(document(entity));
        })
        .run(unit -> {
          Entity r = (Entity) new AsyncDatastoreImpl(unit.get(AsyncBucket.class),
              unit.get(AsyncRepository.class),
              unit.get(Function.class), unit.get(JacksonMapper.class))
                  .upsert()
                  .mutationToken(token)
                  .execute(entity)
                  .toBlocking()
                  .single();
          assertEquals(entity, r);
        }, unit -> {
          EntityDocument doc = unit.captured(EntityDocument.class).iterator().next();
          assertEquals(entity, doc.content());
          assertEquals(token, doc.mutationToken());
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  private Block idGen(final Entity entity, final Object id) {
    return unit -> {
      Function fn = unit.get(Function.class);
      expect(fn.apply(entity)).andReturn(id);
    };
  }

  private Observable<EntityDocument<Entity>> document(final Entity e) {
    return Observable.just(EntityDocument.create(N1Q.qualifyId(e.getClass(), e.id), e));
  }

}
