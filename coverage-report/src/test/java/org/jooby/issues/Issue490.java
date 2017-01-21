package org.jooby.issues;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jooby.AsyncMapper;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue490 extends ServerFeature {

  {
    ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
      Thread thread = new Thread(r, CompletableFuture.class.getSimpleName());
      thread.setDaemon(true);
      return thread;
    });
    onStop(() -> executor.shutdown());
    map(new AsyncMapper());

    get("/490/callable", () -> (Callable<String>) () -> Thread.currentThread().getName());

    get("/490/future", () -> CompletableFuture
        .supplyAsync(() -> Thread.currentThread().getName(), executor));

    get("/490", () -> "OK");

  }

  @Test
  public void shouldMapCallbale() throws Exception {
    request()
        .get("/490/callable")
        .expect(rsp -> {
          System.out.println(rsp);
          assertTrue(rsp.toLowerCase().contains("task"));
        });
  }

  @Test
  public void shouldMapCompletableFuture() throws Exception {
    request()
        .get("/490/future")
        .expect(rsp -> {
          String value = rsp;
          assertTrue(value, CompletableFuture.class.getSimpleName().equals(value));
        });
  }

  @Test
  public void shouldSkip() throws Exception {
    request()
        .get("/490")
        .expect("OK");
  }

}
