package org.jooby.cassandra;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.Result;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Datastore.class, SimpleStatement.class, Futures.class })
public class DatastoreTest {

  public static class Bean {
  }

  @Test
  public void newDatastore() throws Exception {
    new MockUnit(MappingManager.class, Mapper.class)
        .run(unit -> {
          new Datastore(unit.get(MappingManager.class));
        });
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void get() throws Exception {
    Bean bean = new Bean();
    new MockUnit(MappingManager.class, Mapper.class)
        .expect(mapper(Bean.class))
        .expect(unit -> {
          Mapper mapper = unit.get(Mapper.class);
          expect(mapper.get("beanId")).andReturn(bean);
        })
        .run(unit -> {
          Bean result = new Datastore(unit.get(MappingManager.class))
              .get(Bean.class, "beanId");
          assertEquals(bean, result);
        });
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void getAsync() throws Exception {
    ListenableFuture<Bean> bean = Futures.immediateFuture(new Bean());
    new MockUnit(MappingManager.class, Mapper.class)
        .expect(mapper(Bean.class))
        .expect(unit -> {
          Mapper mapper = unit.get(Mapper.class);
          expect(mapper.getAsync("beanId")).andReturn(bean);
        })
        .run(unit -> {
          ListenableFuture<Bean> result = new Datastore(unit.get(MappingManager.class))
              .getAsync(Bean.class, "beanId");
          assertEquals(bean, result);
        });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void delete() throws Exception {
    new MockUnit(MappingManager.class, Mapper.class)
        .expect(mapper(Bean.class))
        .expect(unit -> {
          Mapper mapper = unit.get(Mapper.class);
          mapper.delete("beanId", new Mapper.Option[0]);
        })
        .run(unit -> {
          new Datastore(unit.get(MappingManager.class))
              .delete(Bean.class, "beanId");
        });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void deleteAsync() throws Exception {
    new MockUnit(MappingManager.class, Mapper.class)
        .expect(mapper(Bean.class))
        .expect(unit -> {
          Mapper mapper = unit.get(Mapper.class);
          expect(mapper.deleteAsync("beanId", new Mapper.Option[0])).andReturn(null);
        })
        .run(unit -> {
          new Datastore(unit.get(MappingManager.class))
              .deleteAsync(Bean.class, "beanId");
        });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void deleteAsyncEntity() throws Exception {
    Bean bean = new Bean();
    new MockUnit(MappingManager.class, Mapper.class)
        .expect(mapper(Bean.class))
        .expect(unit -> {
          Mapper mapper = unit.get(Mapper.class);
          expect(mapper.deleteAsync(bean, new Mapper.Option[0])).andReturn(null);
        })
        .run(unit -> {
          new Datastore(unit.get(MappingManager.class))
              .deleteAsync(bean);
        });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void deleteEntity() throws Exception {
    Bean bean = new Bean();
    new MockUnit(MappingManager.class, Mapper.class)
        .expect(mapper(Bean.class))
        .expect(unit -> {
          Mapper mapper = unit.get(Mapper.class);
          mapper.delete(bean, new Mapper.Option[0]);
        })
        .run(unit -> {
          new Datastore(unit.get(MappingManager.class))
              .delete(bean);
        });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void save() throws Exception {
    Bean bean = new Bean();
    new MockUnit(MappingManager.class, Mapper.class)
        .expect(mapper(Bean.class))
        .expect(unit -> {
          Mapper mapper = unit.get(Mapper.class);
          mapper.save(bean, new Mapper.Option[0]);
        })
        .run(unit -> {
          new Datastore(unit.get(MappingManager.class))
              .save(bean);
        });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void saveAsync() throws Exception {
    Bean bean = new Bean();
    new MockUnit(MappingManager.class, Mapper.class)
        .expect(mapper(Bean.class))
        .expect(unit -> {
          Mapper mapper = unit.get(Mapper.class);
          expect(mapper.saveAsync(bean, new Mapper.Option[0])).andReturn(null);
        })
        .run(unit -> {
          new Datastore(unit.get(MappingManager.class))
              .saveAsync(bean);
        });
  }

  @SuppressWarnings({"rawtypes" })
  @Test
  public void queryString() throws Exception {
    new Bean();
    String statement = "select * from beer";
    new MockUnit(MappingManager.class, Mapper.class, Session.class, ResultSet.class, Result.class)
        .expect(mapper(Bean.class))
        .expect(unit -> {
          ResultSet rs = unit.get(ResultSet.class);

          Session session = unit.get(Session.class);
          expect(session.execute(unit.capture(SimpleStatement.class))).andReturn(rs);

          MappingManager manager = unit.get(MappingManager.class);
          expect(manager.getSession()).andReturn(session);

          Mapper mapper = unit.get(Mapper.class);
          expect(mapper.getManager()).andReturn(manager);
          expect(mapper.map(rs)).andReturn(unit.get(Result.class));
        })
        .run(unit -> {
          Result<Bean> r = new Datastore(unit.get(MappingManager.class))
              .query(Bean.class, statement);
          assertEquals(unit.get(Result.class), r);
        }, unit -> {
          SimpleStatement stt = unit.captured(SimpleStatement.class).iterator().next();
          assertEquals(statement, stt.getQueryString());
          assertEquals(0, stt.valuesCount());
        });
  }

  @SuppressWarnings({"rawtypes" })
  @Test
  public void queryStatementMap() throws Exception {
    new Bean();
    String statement = ("select * from beer");
    new MockUnit(MappingManager.class, Mapper.class, Session.class, ResultSet.class, Result.class)
        .expect(mapper(Bean.class))
        .expect(unit -> {
          ResultSet rs = unit.get(ResultSet.class);

          Session session = unit.get(Session.class);
          expect(session.execute(unit.capture(SimpleStatement.class))).andReturn(rs);

          MappingManager manager = unit.get(MappingManager.class);
          expect(manager.getSession()).andReturn(session);

          Mapper mapper = unit.get(Mapper.class);
          expect(mapper.getManager()).andReturn(manager);
          expect(mapper.map(rs)).andReturn(unit.get(Result.class));
        })
        .run(unit -> {
          Result<Bean> r = new Datastore(unit.get(MappingManager.class))
              .query(Bean.class, statement, ImmutableMap.of("foo", "bar"));
          assertEquals(unit.get(Result.class), r);
        }, unit -> {
          SimpleStatement stt = unit.captured(SimpleStatement.class).iterator().next();
          assertEquals(statement, stt.getQueryString());
          assertEquals(1, stt.valuesCount());
          assertEquals(ImmutableSet.of("foo"), stt.getValueNames());
        });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void queryAsyncStatement() throws Exception {
    String statement = ("select * from beer");
    new MockUnit(MappingManager.class, Mapper.class, Session.class, ResultSetFuture.class,
        Result.class, ListenableFuture.class, ResultSet.class)
            .expect(mapper(Bean.class))
            .expect(unit -> {
              ResultSetFuture rs = unit.get(ResultSetFuture.class);

              Session session = unit.get(Session.class);
              expect(session.executeAsync(unit.capture(SimpleStatement.class))).andReturn(rs);

              MappingManager manager = unit.get(MappingManager.class);
              expect(manager.getSession()).andReturn(session);

              Mapper mapper = unit.get(Mapper.class);
              expect(mapper.getManager()).andReturn(manager);
              expect(mapper.map(unit.get(ResultSet.class))).andReturn(unit.get(Result.class));

              unit.mockStatic(Futures.class);
              expect(Futures.transformAsync(eq(rs), unit.capture(AsyncFunction.class)))
                  .andReturn(unit.get(ListenableFuture.class));
              expect(Futures.immediateFuture(unit.get(Result.class)))
                  .andReturn(unit.get(ListenableFuture.class));
            })
            .run(unit -> {
              ListenableFuture<Result<Bean>> r = new Datastore(unit.get(MappingManager.class))
                  .queryAsync(Bean.class, statement);
              assertEquals(unit.get(ListenableFuture.class), r);
            }, unit -> {
              AsyncFunction fn = unit.captured(AsyncFunction.class).iterator().next();
              ListenableFuture f = fn.apply(unit.get(ResultSet.class));
              assertEquals(unit.get(ListenableFuture.class), f);
            }, unit -> {
              SimpleStatement stt = unit.captured(SimpleStatement.class).iterator().next();
              assertEquals(statement, stt.getQueryString());
              assertEquals(0, stt.valuesCount());
            });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void queryAsyncStatementMap() throws Exception {
    String statement = ("select * from beer");
    new MockUnit(MappingManager.class, Mapper.class, Session.class, ResultSetFuture.class,
        Result.class, ListenableFuture.class, ResultSet.class)
            .expect(mapper(Bean.class))
            .expect(unit -> {
              ResultSetFuture rs = unit.get(ResultSetFuture.class);

              Session session = unit.get(Session.class);
              expect(session.executeAsync(unit.capture(SimpleStatement.class))).andReturn(rs);

              MappingManager manager = unit.get(MappingManager.class);
              expect(manager.getSession()).andReturn(session);

              Mapper mapper = unit.get(Mapper.class);
              expect(mapper.getManager()).andReturn(manager);
              expect(mapper.map(unit.get(ResultSet.class))).andReturn(unit.get(Result.class));

              unit.mockStatic(Futures.class);
              expect(Futures.transformAsync(eq(rs), unit.capture(AsyncFunction.class)))
                  .andReturn(unit.get(ListenableFuture.class));
              expect(Futures.immediateFuture(unit.get(Result.class)))
                  .andReturn(unit.get(ListenableFuture.class));
            })
            .run(unit -> {
              ListenableFuture<Result<Bean>> r = new Datastore(unit.get(MappingManager.class))
                  .queryAsync(Bean.class, statement, ImmutableMap.of("foo", "bar"));
              assertEquals(unit.get(ListenableFuture.class), r);
            }, unit -> {
              AsyncFunction fn = unit.captured(AsyncFunction.class).iterator().next();
              ListenableFuture f = fn.apply(unit.get(ResultSet.class));
              assertEquals(unit.get(ListenableFuture.class), f);
            }, unit -> {
              SimpleStatement stt = unit.captured(SimpleStatement.class).iterator().next();
              assertEquals(statement, stt.getQueryString());
              assertEquals(1, stt.valuesCount());
              assertEquals(ImmutableSet.of("foo"), stt.getValueNames());
            });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  private Block mapper(final Class type) {
    return unit -> {
      MappingManager manager = unit.get(MappingManager.class);
      expect(manager.mapper(type)).andReturn(unit.get(Mapper.class));
    };
  }
}
