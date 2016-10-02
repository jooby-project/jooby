package org.jooby.issues;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jooby.AsyncMapper;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue490b extends ServerFeature {

  {
    map(new AsyncMapper());

    ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
      Thread thread = new Thread(r);
      thread.setName("490");
      return thread;
    });
    executor(exec);

    get("/490/callable", () -> (Callable<String>) () -> Thread.currentThread().getName());

    get("/490/future",
        () -> CompletableFuture.supplyAsync(() -> Thread.currentThread().getName(), exec));

  }

  @Test
  public void shouldMapCallbale() throws Exception {
    request()
        .get("/490/callable")
        .expect(rsp -> {
          assertEquals("490", rsp);
        });

    request()
        .get("/490/future")
        .expect(rsp -> {
          assertEquals("490", rsp);
        });
  }
}
