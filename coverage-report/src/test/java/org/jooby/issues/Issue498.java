package org.jooby.issues;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.couchbase.client.deps.io.netty.util.internal.chmv8.ForkJoinPool;

public class Issue498 extends ServerFeature {

  {
    executor(new ForkJoinPool());

    get("/498", deferred(req -> {
      assertNotNull(req);
      return Thread.currentThread().getName();
    }));

    get("/498/0", deferred(() -> {
      return Thread.currentThread().getName();
    }));

    get("/498/x", deferred(() -> {
      throw new IllegalStateException("intentional err");
    }));

    err((req, rsp, x) -> {
      rsp.send(x.getCause().getMessage());
    });
  }

  @Test
  public void functionalDeferred() throws Exception {
    request()
        .get("/498")
        .expect(v -> {
          assertTrue(v.toLowerCase().contains("fork"));
        });

    request()
        .get("/498/0")
        .expect(v -> {
          assertTrue(v.toLowerCase().contains("fork"));
        });

    request()
        .get("/498/x")
        .expect("intentional err");
  }

}
