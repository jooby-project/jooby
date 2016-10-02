package org.jooby.issues;

import static org.junit.Assert.assertNotEquals;

import java.util.concurrent.ForkJoinPool;

import org.jooby.Deferred;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue484b extends ServerFeature {

  {
    executor(new ForkJoinPool());

    get("/484", req -> {
      return new Deferred(deferred -> {
        deferred.resolve(deferred.callerThread() + ":" + Thread.currentThread().getName());
      });
    });

    get("/484/promise", promise(deferred -> {
      deferred.resolve(deferred.callerThread() + ":" + Thread.currentThread().getName());
    }));
  }

  @Test
  public void deferredWithExecutorInstance() throws Exception {
    request()
        .get("/484")
        .expect(rsp -> {
          System.out.println(rsp);
          String[] threads = rsp.split(":");
          assertNotEquals(threads[0], threads[1]);
        });

    request()
        .get("/484/promise")
        .expect(rsp -> {
          System.out.println(rsp);
          String[] threads = rsp.split(":");
          assertNotEquals(threads[0], threads[1]);
        });
  }
}
