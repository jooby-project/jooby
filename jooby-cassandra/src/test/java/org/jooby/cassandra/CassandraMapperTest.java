package org.jooby.cassandra;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.jooby.Deferred;
import org.jooby.Request;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.mapping.Result;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CassandraMapper.class, Futures.class })
public class CassandraMapperTest {

  @SuppressWarnings("unchecked")
  private Block futureCallback = unit -> {
    unit.mockStatic(Futures.class);
    Futures.addCallback(eq(unit.get(ListenableFuture.class)),
        unit.capture(FutureCallback.class));
  };

  @Test
  public void name() {
    assertEquals("cassandra", new CassandraMapper().name());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void resultSet() throws Exception {
    new MockUnit(Request.class, ResultSet.class, Row.class)
        .expect(unit -> {
          ResultSet rs = unit.get(ResultSet.class);
          expect(rs.all()).andReturn(ImmutableList.of(unit.get(Row.class)));
        })
        .run(unit -> {
          List<ResultSet> rs = (List<ResultSet>) new CassandraMapper()
              .map(unit.get(ResultSet.class));
          assertEquals(ImmutableList.of(unit.get(Row.class)), rs);
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void result() throws Exception {
    new MockUnit(Request.class, Result.class, Row.class)
        .expect(unit -> {
          Result rs = unit.get(Result.class);
          expect(rs.all()).andReturn(ImmutableList.of(unit.get(Row.class)));
        })
        .run(unit -> {
          List<ResultSet> rs = (List<ResultSet>) new CassandraMapper()
              .map(unit.get(Result.class));
          assertEquals(ImmutableList.of(unit.get(Row.class)), rs);
        });
  }

  @Test
  public void ignored() throws Exception {
    Object value = new Object();
    new MockUnit(Request.class, Result.class, Row.class)
        .run(unit -> {
          Object result = new CassandraMapper()
              .map(value);
          assertEquals(value, result);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void listeneableFutureSuccess() throws Exception {
    Object value = new Object();
    CountDownLatch latch = new CountDownLatch(1);
    new MockUnit(Request.class, ListenableFuture.class)
        .expect(futureCallback)
        .run(unit -> {
          Deferred deferred = (Deferred) new CassandraMapper()
              .map(unit.get(ListenableFuture.class));
          deferred.handler(unit.get(Request.class), (result, x) -> {
            assertEquals(value, result.get());
            latch.countDown();
          });
        }, unit -> {
          unit.captured(FutureCallback.class).iterator().next().onSuccess(value);
        });
    latch.await();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void listeneableFutureResultSet() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    new MockUnit(Request.class, ListenableFuture.class, ResultSet.class, Row.class)
        .expect(futureCallback)
        .expect(unit -> {
          ResultSet rs = unit.get(ResultSet.class);
          expect(rs.all()).andReturn(ImmutableList.of(unit.get(Row.class)));
        })
        .run(unit -> {
          Deferred deferred = (Deferred) new CassandraMapper()
              .map(unit.get(ListenableFuture.class));
          deferred.handler(unit.get(Request.class), (result, x) -> {
            assertEquals(ImmutableList.of(unit.get(Row.class)), result.get());
            latch.countDown();
          });
        }, unit -> {
          unit.captured(FutureCallback.class).iterator().next()
              .onSuccess(unit.get(ResultSet.class));
        });
    latch.await();
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void listeneableFutureResult() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    new MockUnit(Request.class, ListenableFuture.class, Result.class, Row.class)
        .expect(futureCallback)
        .expect(unit -> {
          Result rs = unit.get(Result.class);
          expect(rs.all()).andReturn(ImmutableList.of(unit.get(Row.class)));
        })
        .run(unit -> {
          Deferred deferred = (Deferred) new CassandraMapper()
              .map(unit.get(ListenableFuture.class));
          deferred.handler(unit.get(Request.class), (result, x) -> {
            assertEquals(ImmutableList.of(unit.get(Row.class)), result.get());
            latch.countDown();
          });
        }, unit -> {
          unit.captured(FutureCallback.class).iterator().next()
              .onSuccess(unit.get(Result.class));
        });
    latch.await();
  }

  @Test
  public void listeneableFutureError() throws Exception {
    Throwable value = new Throwable();
    CountDownLatch latch = new CountDownLatch(1);
    new MockUnit(Request.class, ListenableFuture.class)
        .expect(futureCallback)
        .run(unit -> {
          Deferred deferred = (Deferred) new CassandraMapper()
              .map(unit.get(ListenableFuture.class));
          deferred.handler(unit.get(Request.class), (result, x) -> {
            assertEquals(value, x);
            latch.countDown();
          });
        }, unit -> {
          unit.captured(FutureCallback.class).iterator().next().onFailure(value);
        });
    latch.await();
  }
}
