package org.jooby.issues;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue485 extends ServerFeature {

  {
    executor(new ForkJoinPool());
    executor("cached", Executors.newCachedThreadPool());

    get("/485/fj", promise(deferred -> {
      deferred.resolve(Thread.currentThread().getName());
    }));

    get("/485/cached", promise("cached", deferred -> {
      deferred.resolve(Thread.currentThread().getName());
    }));

  }

  @Test
  public void globalOrLocalExecutor() throws Exception {
    request()
        .get("/485/fj")
        .expect(rsp -> {
          assertTrue(rsp.toLowerCase().startsWith("forkjoinpool"));
        });

    request()
        .get("/485/cached")
        .expect(rsp -> {
          assertTrue(rsp.toLowerCase().startsWith("pool"));
        });
  }
}
