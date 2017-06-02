package org.jooby.internal.couchbase;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.jooby.couchbase.AsyncDatastore;
import org.jooby.couchbase.AsyncDatastore.AsyncCommand;
import org.jooby.couchbase.Datastore.ViewQueryResult;
import org.jooby.test.MockUnit;
import org.junit.Test;

import com.couchbase.client.core.message.kv.MutationToken;
import com.couchbase.client.java.PersistTo;
import com.couchbase.client.java.ReplicaMode;
import com.couchbase.client.java.ReplicateTo;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.view.ViewQuery;

import rx.Observable;

@SuppressWarnings("rawtypes")
public class DatastoreImplTest {

  @Test
  public void async() throws Exception {
    new MockUnit(AsyncDatastore.class)
        .run(unit -> {
          AsyncDatastore async = new DatastoreImpl(unit.get(AsyncDatastore.class))
              .async();
          assertEquals(unit.get(AsyncDatastore.class), async);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void get() throws Exception {
    String gid = "id";
    Object entity = new Object() {
      String id = gid;

      @Override
      public String toString() {
        return id;
      }
    };
    new MockUnit(AsyncDatastore.class)
        .expect(unit -> {
          AsyncDatastore store = unit.get(AsyncDatastore.class);
          expect(store.get(entity.getClass(), gid)).andReturn(just(entity));
        })
        .run(unit -> {
          Object result = new DatastoreImpl(unit.get(AsyncDatastore.class))
              .get(entity.getClass(), gid);
          assertEquals(entity, result);
        });
  }

  @Test(expected = DocumentDoesNotExistException.class)
  public void getNotFound() throws Exception {
    String gid = "id";
    Object entity = new Object() {
      String id = gid;

      @Override
      public String toString() {
        return id;
      }
    };
    new MockUnit(AsyncDatastore.class)
        .expect(unit -> {
          AsyncDatastore store = unit.get(AsyncDatastore.class);
          expect(store.get(entity.getClass(), gid)).andReturn(Observable.empty());
        })
        .run(unit -> {
          new DatastoreImpl(unit.get(AsyncDatastore.class))
              .get(entity.getClass(), gid);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void getFromReplica() throws Exception {
    String gid = "id";
    Object entity = new Object() {
      String id = gid;

      @Override
      public String toString() {
        return id;
      }
    };
    new MockUnit(AsyncDatastore.class)
        .expect(unit -> {
          AsyncDatastore store = unit.get(AsyncDatastore.class);
          expect(store.getFromReplica(entity.getClass(), gid, ReplicaMode.FIRST))
              .andReturn(just(entity));
        })
        .run(unit -> {
          Object result = new DatastoreImpl(unit.get(AsyncDatastore.class))
              .getFromReplica(entity.getClass(), gid, ReplicaMode.FIRST);
          assertEquals(entity, result);
        });
  }

  @Test(expected = DocumentDoesNotExistException.class)
  public void getFromReplicaNotFound() throws Exception {
    String gid = "id";
    Object entity = new Object() {
      String id = gid;

      @Override
      public String toString() {
        return id;
      }
    };
    new MockUnit(AsyncDatastore.class)
        .expect(unit -> {
          AsyncDatastore store = unit.get(AsyncDatastore.class);
          expect(store.getFromReplica(entity.getClass(), gid, ReplicaMode.FIRST))
              .andReturn(Observable.empty());
        })
        .run(unit -> {
          new DatastoreImpl(unit.get(AsyncDatastore.class))
              .getFromReplica(entity.getClass(), gid, ReplicaMode.FIRST);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void getAndLock() throws Exception {
    String gid = "id";
    Object entity = new Object() {
      String id = gid;

      @Override
      public String toString() {
        return id;
      }
    };
    new MockUnit(AsyncDatastore.class)
        .expect(unit -> {
          AsyncDatastore store = unit.get(AsyncDatastore.class);
          expect(store.getAndLock(entity.getClass(), gid, 1))
              .andReturn(just(entity));
        })
        .run(unit -> {
          Object result = new DatastoreImpl(unit.get(AsyncDatastore.class))
              .getAndLock(entity.getClass(), gid, 1);
          assertEquals(entity, result);
        });
  }

  @Test(expected = DocumentDoesNotExistException.class)
  public void getAndLockNotFound() throws Exception {
    String gid = "id";
    Object entity = new Object() {
      String id = gid;

      @Override
      public String toString() {
        return id;
      }
    };
    new MockUnit(AsyncDatastore.class)
        .expect(unit -> {
          AsyncDatastore store = unit.get(AsyncDatastore.class);
          expect(store.getAndLock(entity.getClass(), gid, 1))
              .andReturn(Observable.empty());
        })
        .run(unit -> {
          new DatastoreImpl(unit.get(AsyncDatastore.class))
              .getAndLock(entity.getClass(), gid, 1);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void getAndTouch() throws Exception {
    String gid = "id";
    Object entity = new Object() {
      String id = gid;

      @Override
      public String toString() {
        return id;
      }
    };
    new MockUnit(AsyncDatastore.class)
        .expect(unit -> {
          AsyncDatastore store = unit.get(AsyncDatastore.class);
          expect(store.getAndTouch(entity.getClass(), gid, 1))
              .andReturn(just(entity));
        })
        .run(unit -> {
          Object result = new DatastoreImpl(unit.get(AsyncDatastore.class))
              .getAndTouch(entity.getClass(), gid, 1);
          assertEquals(entity, result);
        });
  }

  @Test(expected = DocumentDoesNotExistException.class)
  public void getAndTouchNotFound() throws Exception {
    String gid = "id";
    Object entity = new Object() {
      String id = gid;

      @Override
      public String toString() {
        return id;
      }
    };
    new MockUnit(AsyncDatastore.class)
        .expect(unit -> {
          AsyncDatastore store = unit.get(AsyncDatastore.class);
          expect(store.getAndTouch(entity.getClass(), gid, 1))
              .andReturn(Observable.empty());
        })
        .run(unit -> {
          new DatastoreImpl(unit.get(AsyncDatastore.class))
              .getAndTouch(entity.getClass(), gid, 1);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void exists() throws Exception {
    String gid = "id";
    Object entity = new Object() {
      String id = gid;

      @Override
      public String toString() {
        return id;
      }
    };
    new MockUnit(AsyncDatastore.class)
        .expect(unit -> {
          AsyncDatastore store = unit.get(AsyncDatastore.class);
          expect(store.exists(entity.getClass(), gid))
              .andReturn(just(true));
        })
        .run(unit -> {
          Object result = new DatastoreImpl(unit.get(AsyncDatastore.class))
              .exists(entity.getClass(), gid);
          assertEquals(true, result);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void query() throws Exception {
    String gid = "id";
    Object entity = new Object() {
      String id = gid;

      @Override
      public String toString() {
        return id;
      }
    };

    new MockUnit(AsyncDatastore.class, N1qlQuery.class)
        .expect(unit -> {
          AsyncDatastore store = unit.get(AsyncDatastore.class);
          expect(store.query(unit.get(N1qlQuery.class)))
              .andReturn(just(Arrays.asList(entity)));
        })
        .run(unit -> {
          List<Object> result = new DatastoreImpl(unit.get(AsyncDatastore.class))
              .query(unit.get(N1qlQuery.class));
          assertEquals(Arrays.asList(entity), result);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void queryStmt() throws Exception {
    String gid = "id";
    Object entity = new Object() {
      String id = gid;

      @Override
      public String toString() {
        return id;
      }
    };

    new MockUnit(AsyncDatastore.class, Statement.class)
        .expect(unit -> {
          AsyncDatastore store = unit.get(AsyncDatastore.class);
          expect(store.query(unit.get(Statement.class)))
              .andReturn(just(Arrays.asList(entity)));
        })
        .run(unit -> {
          List<Object> result = new DatastoreImpl(unit.get(AsyncDatastore.class))
              .query(unit.get(Statement.class));
          assertEquals(Arrays.asList(entity), result);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void viewQuery() throws Exception {
    String gid = "id";
    Object entity = new Object() {
      String id = gid;

      @Override
      public String toString() {
        return id;
      }
    };

    new MockUnit(AsyncDatastore.class, ViewQuery.class)
        .expect(unit -> {
          AsyncDatastore store = unit.get(AsyncDatastore.class);
          expect(store.query(unit.get(ViewQuery.class)))
              .andReturn(just(
                  new AsyncDatastore.AsyncViewQueryResult<Object>(10,
                      just(Arrays.asList(entity)))));
        })
        .run(unit -> {
          ViewQueryResult<Object> result = new DatastoreImpl(unit.get(AsyncDatastore.class))
              .query(unit.get(ViewQuery.class));
          assertEquals(Arrays.asList(entity), result.getRows());
          assertEquals(10, result.getTotalRows());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void upsert() throws Exception {
    String gid = "id";
    Object entity = new Object() {
      String id = gid;

      @Override
      public String toString() {
        return id;
      }
    };
    new MockUnit(AsyncDatastore.class, AsyncCommand.class)
        .expect(unit -> {
          AsyncCommand cmd = unit.get(AsyncCommand.class);
          expect(cmd.execute(entity, PersistTo.NONE, ReplicateTo.NONE)).andReturn(just(entity));

          AsyncDatastore store = unit.get(AsyncDatastore.class);
          expect(store.upsert()).andReturn(cmd);
        })
        .run(unit -> {
          Object result = new DatastoreImpl(unit.get(AsyncDatastore.class))
              .upsert(entity);
          assertEquals(entity, result);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void upsertWithReplicate() throws Exception {
    String gid = "id";
    Object entity = new Object() {
      String id = gid;

      @Override
      public String toString() {
        return id;
      }
    };
    new MockUnit(AsyncDatastore.class, AsyncCommand.class)
        .expect(unit -> {
          AsyncCommand cmd = unit.get(AsyncCommand.class);
          expect(cmd.execute(entity, PersistTo.NONE, ReplicateTo.ONE)).andReturn(just(entity));

          AsyncDatastore store = unit.get(AsyncDatastore.class);
          expect(store.upsert()).andReturn(cmd);
        })
        .run(unit -> {
          Object result = new DatastoreImpl(unit.get(AsyncDatastore.class))
              .upsert()
              .execute(entity, ReplicateTo.ONE);
          assertEquals(entity, result);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void insert() throws Exception {
    String gid = "id";
    Object entity = new Object() {
      String id = gid;

      @Override
      public String toString() {
        return id;
      }
    };
    new MockUnit(AsyncDatastore.class, AsyncCommand.class)
        .expect(unit -> {
          AsyncCommand cmd = unit.get(AsyncCommand.class);
          expect(cmd.execute(entity, PersistTo.NONE, ReplicateTo.NONE)).andReturn(just(entity));

          AsyncDatastore store = unit.get(AsyncDatastore.class);
          expect(store.insert()).andReturn(cmd);
        })
        .run(unit -> {
          Object result = new DatastoreImpl(unit.get(AsyncDatastore.class))
              .insert(entity);
          assertEquals(entity, result);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void replace() throws Exception {
    String gid = "id";
    Object entity = new Object() {
      String id = gid;

      @Override
      public String toString() {
        return id;
      }
    };
    new MockUnit(AsyncDatastore.class, AsyncCommand.class)
        .expect(unit -> {
          AsyncCommand cmd = unit.get(AsyncCommand.class);
          expect(cmd.execute(entity, PersistTo.NONE, ReplicateTo.NONE)).andReturn(just(entity));

          AsyncDatastore store = unit.get(AsyncDatastore.class);
          expect(store.replace()).andReturn(cmd);
        })
        .run(unit -> {
          Object result = new DatastoreImpl(unit.get(AsyncDatastore.class))
              .replace(entity);
          assertEquals(entity, result);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void remove() throws Exception {
    String gid = "id";
    Object entity = new Object() {
      String id = gid;

      @Override
      public String toString() {
        return id;
      }
    };
    new MockUnit(AsyncDatastore.class, AsyncDatastore.AsyncRemoveCommand.class)
        .expect(unit -> {
          AsyncDatastore.AsyncRemoveCommand cmd = unit.get(AsyncDatastore.AsyncRemoveCommand.class);
          expect(cmd.execute(entity, PersistTo.NONE, ReplicateTo.NONE)).andReturn(just(1L));

          AsyncDatastore store = unit.get(AsyncDatastore.class);
          expect(store.remove()).andReturn(cmd);
        })
        .run(unit -> {
          long result = new DatastoreImpl(unit.get(AsyncDatastore.class))
              .remove(entity);
          assertEquals(1L, result);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void removeWithReplicate() throws Exception {
    String gid = "id";
    Object entity = new Object() {
      String id = gid;

      @Override
      public String toString() {
        return id;
      }
    };
    new MockUnit(AsyncDatastore.class, AsyncDatastore.AsyncRemoveCommand.class)
        .expect(unit -> {
          AsyncDatastore.AsyncRemoveCommand cmd = unit.get(AsyncDatastore.AsyncRemoveCommand.class);
          expect(cmd.execute(entity, PersistTo.NONE, ReplicateTo.ONE)).andReturn(just(1L));

          AsyncDatastore store = unit.get(AsyncDatastore.class);
          expect(store.remove()).andReturn(cmd);
        })
        .run(unit -> {
          long result = new DatastoreImpl(unit.get(AsyncDatastore.class))
              .remove()
              .execute(entity, ReplicateTo.ONE);
          assertEquals(1L, result);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void removeById() throws Exception {
    String gid = "id";
    Object entity = new Object() {
      String id = gid;

      @Override
      public String toString() {
        return id;
      }
    };
    new MockUnit(AsyncDatastore.class, AsyncDatastore.AsyncRemoveCommand.class)
        .expect(unit -> {
          AsyncDatastore.AsyncRemoveCommand cmd = unit.get(AsyncDatastore.AsyncRemoveCommand.class);
          expect(cmd.execute(entity.getClass(), gid, PersistTo.NONE, ReplicateTo.NONE))
              .andReturn(just(1L));

          AsyncDatastore store = unit.get(AsyncDatastore.class);
          expect(store.remove()).andReturn(cmd);
        })
        .run(unit -> {
          long result = new DatastoreImpl(unit.get(AsyncDatastore.class))
              .remove(entity.getClass(), gid);
          assertEquals(1L, result);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void removeByIdWithReplicate() throws Exception {
    String gid = "id";
    Object entity = new Object() {
      String id = gid;

      @Override
      public String toString() {
        return id;
      }
    };
    new MockUnit(AsyncDatastore.class, AsyncDatastore.AsyncRemoveCommand.class)
        .expect(unit -> {
          AsyncDatastore.AsyncRemoveCommand cmd = unit.get(AsyncDatastore.AsyncRemoveCommand.class);
          expect(cmd.execute(entity.getClass(), gid, PersistTo.NONE, ReplicateTo.ONE))
              .andReturn(just(1L));

          AsyncDatastore store = unit.get(AsyncDatastore.class);
          expect(store.remove()).andReturn(cmd);
        })
        .run(unit -> {
          long result = new DatastoreImpl(unit.get(AsyncDatastore.class))
              .remove()
              .execute(entity.getClass(), gid, ReplicateTo.ONE);
          assertEquals(1L, result);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void commandprops() throws Exception {
    String gid = "id";
    Object entity = new Object() {
      String id = gid;

      @Override
      public String toString() {
        return id;
      }
    };
    MutationToken token = new MutationToken(0, 0, 0, gid);
    new MockUnit(AsyncDatastore.class, AsyncCommand.class)
        .expect(unit -> {
          AsyncCommand cmd = unit.get(AsyncCommand.class);
          expect(cmd.cas(10L)).andReturn(cmd);
          expect(cmd.expiry(3)).andReturn(cmd);
          expect(cmd.mutationToken(token)).andReturn(cmd);
          expect(cmd.execute(entity, PersistTo.NONE, ReplicateTo.NONE)).andReturn(just(entity));

          AsyncDatastore store = unit.get(AsyncDatastore.class);
          expect(store.upsert()).andReturn(cmd);
        })
        .run(unit -> {
          Object result = new DatastoreImpl(unit.get(AsyncDatastore.class))
              .upsert()
              .cas(10L)
              .expiry(3)
              .mutationToken(token)
              .execute(entity);
          assertEquals(entity, result);
        });
  }

  private Observable just(final Object entity) {
    return Observable.just(entity);
  }

}
