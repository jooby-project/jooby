package org.jooby.issues;

import static org.junit.Assert.assertTrue;

import org.jooby.Deferred;
import org.jooby.exec.Exec;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue488d extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("executors.fj", ConfigValueFactory.fromAnyRef("forkjoin = 2"))
        .withValue("executors.cached", ConfigValueFactory.fromAnyRef("cached")));

    executor("fj");

    use(new Exec());

    get("/488", req -> new Deferred(deferred -> {
      deferred.resolve(Thread.currentThread().getName());
    }));

    get("/488/cached", req -> new Deferred("cached", deferred -> {
      deferred.resolve(Thread.currentThread().getName());
    }));

    get("/488/fj", promise(deferred -> {
      deferred.resolve(Thread.currentThread().getName());
    }));

    get("/488/local/cached", promise("cached", (req, deferred) -> {
      deferred.resolve(Thread.currentThread().getName());
    }));

    get("/488/local/fj", promise("fj", deferred -> {
      deferred.resolve(Thread.currentThread().getName());
    }));
  }

  @Test
  public void deferredOnGloablOrLocalExecutor() throws Exception {
    request()
        .get("/488")
        .expect(rsp -> {
          assertTrue(rsp.startsWith("forkjoin"));
        });

    request()
        .get("/488/cached")
        .expect(rsp -> {
          assertTrue(rsp.startsWith("cached"));
        });

    request()
        .get("/488/fj")
        .expect(rsp -> {
          assertTrue(rsp.startsWith("forkjoin"));
        });

    request()
        .get("/488/local/cached")
        .expect(rsp -> {
          assertTrue(rsp.startsWith("cached"));
        });

    request()
        .get("/488/local/fj")
        .expect(rsp -> {
          assertTrue(rsp.startsWith("forkjoin"));
        });
  }
}
