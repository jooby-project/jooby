package org.jooby.issues;

import static org.junit.Assert.assertEquals;

import org.jooby.Deferred;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue484 extends ServerFeature {

  {
    get("/484", req -> {
      String t1 = Thread.currentThread().getName();
      return new Deferred(deferred -> {
        deferred.resolve(t1 + ":" + Thread.currentThread().getName());
      });
    });

    get("/484/promise", promise(deferred -> {
      String t1 = Thread.currentThread().getName();
      deferred.resolve(t1 + ":" + Thread.currentThread().getName());
    }));
  }

  @Test
  public void deferredOnDefaultExecutor() throws Exception {
    request()
        .get("/484")
        .expect(rsp -> {
          String[] threads = rsp.split(":");
          assertEquals(threads[0], threads[1]);
        });

    request()
        .get("/484/promise")
        .expect(rsp -> {
          String[] threads = rsp.split(":");
          assertEquals(threads[0], threads[1]);
        });
  }
}
