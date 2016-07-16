package org.jooby.issues;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.jooby.Results;
import org.jooby.test.OnServer;
import org.jooby.test.ServerFeature;
import org.jooby.undertow.Undertow;
import org.junit.After;
import org.junit.Test;

@OnServer(Undertow.class)
public class Issue423 extends ServerFeature {

  private static final AtomicReference<String> T = new AtomicReference<>(null);

  private static final CountDownLatch latch = new CountDownLatch(1);

  {
    ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "deferred");
      t.setDaemon(true);
      return t;
    });

    after((req, rsp, result) -> {
      return Results.ok("async:" + result.get());
    });

    after((req, rsp, result) -> {
      Thread thread = Thread.currentThread();
      return Results.ok(thread.getName() + ":" + result.get());
    });

    complete((req, rsp, err) -> {
      Thread thread = Thread.currentThread();
      T.set(thread.getName());
      latch.countDown();
    });

    get("/423", promise(deferred -> {
      executor.execute(() -> {
        deferred.resolve("foo");
      });
    }));
  }

  @Test
  public void afterCompleteCallbacksShouldRunInAsyncMode() throws Exception {
    request()
        .get("/423")
        .expect("async:deferred:foo");
  }

  @After
  public void after() throws InterruptedException {
    latch.await();
    assertEquals("deferred", T.getAndSet(null));
  }

}
