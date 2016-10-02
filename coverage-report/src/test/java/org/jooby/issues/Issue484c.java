package org.jooby.issues;

import static org.junit.Assert.assertNotEquals;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jooby.Deferred;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.inject.Key;
import com.google.inject.name.Names;

public class Issue484c extends ServerFeature {

  {
    executor("ste");

    use((env, conf, binder) -> {
      binder.bind(Key.get(Executor.class, Names.named("ste")))
          .toInstance(Executors.newSingleThreadExecutor());
    });

    get("/484", req -> {
      return new Deferred(deferred -> {
        deferred.resolve(deferred.callerThread() + ":" + Thread.currentThread().getName());
      });
    });

    get("/484/promise", promise((req, deferred) -> {
      deferred.resolve(deferred.callerThread() + ":" + Thread.currentThread().getName());
    }));
  }

  @Test
  public void deferredWithExecutorReference() throws Exception {
    request()
        .get("/484")
        .expect(rsp -> {
          String[] threads = rsp.split(":");
          assertNotEquals(threads[0], threads[1]);
        });

    request()
        .get("/484/promise")
        .expect(rsp -> {
          String[] threads = rsp.split(":");
          assertNotEquals(threads[0], threads[1]);
        });
  }
}
