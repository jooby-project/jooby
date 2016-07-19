package org.jooby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.CountDownLatch;

import org.jooby.test.MockUnit;
import org.junit.Test;

public class DeferredTest {

  @Test
  public void newWithNoInit() throws Exception {
    new Deferred().handler(null, (r, ex) -> {
    });
  }

  @Test
  public void newWithInit0() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    new Deferred(deferred -> {
      assertNotNull(deferred);
      latch.countDown();
    }).handler(null, (r, ex) -> {
    });
    latch.await();
  }

  @Test
  public void newWithInit() throws Exception {
    new MockUnit(Request.class)
        .run(unit -> {
          CountDownLatch latch = new CountDownLatch(1);
          new Deferred((req, deferred) -> {
            assertNotNull(deferred);
            assertEquals(unit.get(Request.class), req);
            latch.countDown();
          }).handler(unit.get(Request.class), (r, ex) -> {
          });
          latch.await();
        });
  }

  @Test
  public void resolve() throws Exception {
    Object value = new Object();
    CountDownLatch latch = new CountDownLatch(1);
    Deferred deferred = new Deferred();
    deferred.handler(null, (result, ex) -> {
      assertFalse(result instanceof Deferred);
      assertEquals(value, result.ifGet().get());
      assertNull(ex);
      latch.countDown();
    });
    deferred.resolve(value);
    latch.await();
  }

  @Test
  public void setResolve() throws Exception {
    Object value = new Object();
    CountDownLatch latch = new CountDownLatch(1);
    Deferred deferred = new Deferred();
    deferred.handler(null, (result, ex) -> {
      assertFalse(result instanceof Deferred);
      assertEquals(value, result.ifGet().get());
      latch.countDown();
    });
    deferred.set(value);
    latch.await();
  }

  @Test
  public void reject() throws Exception {
    Exception cause = new Exception();
    CountDownLatch latch = new CountDownLatch(1);
    Deferred deferred = new Deferred();
    deferred.handler(null, (result, ex) -> {
      assertEquals(cause, ex);
      assertNull(result);
      latch.countDown();
    });
    deferred.reject(cause);
    latch.await();
  }

  @Test
  public void setReject() throws Exception {
    Exception cause = new Exception();
    CountDownLatch latch = new CountDownLatch(1);
    Deferred deferred = new Deferred();
    deferred.handler(null, (result, ex) -> {
      assertEquals(cause, ex);
      assertNull(result);
      latch.countDown();
    });
    deferred.set(cause);
    latch.await();
  }

  @Test
  public void runResolve() throws Exception {
    Object value = new Object();
    CountDownLatch latch = new CountDownLatch(1);
    Deferred deferred = new Deferred();
    deferred.handler(null, (result, ex) -> {
      assertFalse(result instanceof Deferred);
      assertEquals(value, result.ifGet().get());
      latch.countDown();
    });
    deferred.run(() -> value).run();
    latch.await();
  }

  @Test
  public void runReject() throws Exception {
    Exception cause = new Exception();
    CountDownLatch latch = new CountDownLatch(1);
    Deferred deferred = new Deferred();
    deferred.handler(null, (result, ex) -> {
      assertEquals(cause, ex);
      assertNull(result);
      latch.countDown();
    });
    deferred.run(() -> {
      throw cause;
    }).run();
    latch.await();
  }

  @Test
  public void callableResolve() throws Exception {
    Object value = new Object();
    CountDownLatch latch = new CountDownLatch(1);
    Deferred deferred = new Deferred();
    deferred.handler(null, (result, ex) -> {
      assertFalse(result instanceof Deferred);
      assertEquals(value, result.ifGet().get());
      latch.countDown();
    });
    deferred.resolve(() -> value);
    latch.await();
  }

  @Test
  public void callableReject() throws Exception {
    Exception cause = new Exception();
    CountDownLatch latch = new CountDownLatch(1);
    Deferred deferred = new Deferred();
    deferred.handler(null, (result, ex) -> {
      assertEquals(cause, ex);
      assertNull(result);
      latch.countDown();
    });
    deferred.resolve(() -> {
      throw cause;
    });
    latch.await();
  }

}
