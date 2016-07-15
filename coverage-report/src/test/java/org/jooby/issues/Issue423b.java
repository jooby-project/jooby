package org.jooby.issues;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.jooby.test.OnServer;
import org.jooby.test.ServerFeature;
import org.jooby.undertow.Undertow;
import org.junit.After;
import org.junit.Test;

@OnServer(Undertow.class)
public class Issue423b extends ServerFeature {

  private static final AtomicReference<String> T = new AtomicReference<>(null);
  {
    ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "deferred");
      t.setDaemon(true);
      return t;
    });

    renderer((v, ctx) -> {
      throw new IllegalStateException("renderer-err");
    });
    complete((req, rsp, err) -> {
      Thread thread = Thread.currentThread();
      T.set(thread.getName() + ":" + err.get().getMessage());
    });

    get("/423", promise(deferred -> {
      executor.execute(() -> {
        deferred.resolve("foo");
      });
    }));
  }

  @Test
  public void shouldImportStartStopCallback() throws Exception {
    request()
        .get("/423")
        .expect(500);
  }

  @After
  public void after() {
    assertEquals("deferred:renderer-err", T.getAndSet(null));
  }

}
