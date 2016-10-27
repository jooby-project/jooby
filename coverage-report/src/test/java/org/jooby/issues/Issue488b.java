package org.jooby.issues;

import static org.junit.Assert.assertNotEquals;

import java.util.concurrent.ForkJoinPool;

import org.jooby.Deferred;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue488b extends ServerFeature {

  {
    executor(new ForkJoinPool());

    get("/488", req -> {
      return new Deferred(deferred -> {
        deferred.resolve(deferred.callerThread() + ":" + Thread.currentThread().getName());
      });
    });

    get("/488/promise", promise(deferred -> {
      deferred.resolve(deferred.callerThread() + ":" + Thread.currentThread().getName());
    }));
  }

  @Test
  public void deferredWithExecutorInstance() throws Exception {
    request()
        .get("/488")
        .expect(rsp -> {
          String[] threads = rsp.split(":");
          assertNotEquals(threads[0], threads[1]);
        });

    request()
        .get("/488/promise")
        .expect(rsp -> {
          String[] threads = rsp.split(":");
          assertNotEquals(threads[0], threads[1]);
        });
  }
}
