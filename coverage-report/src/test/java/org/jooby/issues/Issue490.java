package org.jooby.issues;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import org.jooby.AsyncMapper;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue490 extends ServerFeature {

  {
    map(new AsyncMapper());

    get("/490/callable", () -> (Callable<String>) () -> Thread.currentThread().getName());

    get("/490/future", () -> CompletableFuture
        .supplyAsync(() -> Thread.currentThread().getName()));

    get("/490", () -> "OK");

  }

  @Test
  public void shouldMapCallbale() throws Exception {
    request()
        .get("/490/callable")
        .expect(rsp -> {
          assertTrue(rsp.toLowerCase().contains("task"));
        });
  }

  @Test
  public void shouldMapCompletableFuture() throws Exception {
    request()
        .get("/490/future")
        .expect(rsp -> {
          assertTrue(rsp.toLowerCase().startsWith("forkjoinpool"));
        });
  }

  @Test
  public void shouldSkip() throws Exception {
    request()
        .get("/490")
        .expect("OK");
  }

}
