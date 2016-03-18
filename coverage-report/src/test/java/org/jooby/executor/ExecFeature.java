package org.jooby.executor;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

import org.jooby.exec.Exec;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class ExecFeature extends ServerFeature {

  {

    use(ConfigFactory.empty().withValue("executors",
        ConfigValueFactory.fromAnyRef("forkjoin = 2")));

    use(new Exec());

    get("/forkjoin", promise((req, deferred) -> {
      req.require(Executor.class);
      req.require(ExecutorService.class);
      ForkJoinPool executor = req.require(ForkJoinPool.class);
      executor.execute(() -> {
        deferred.resolve(Thread.currentThread().getName());
      });
    }));
  }

  @Test
  public void forkJoinPool() throws Exception {
    request().get("/forkjoin").expect("forkjoin");
  }
}
