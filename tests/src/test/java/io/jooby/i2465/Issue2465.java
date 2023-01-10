/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2465;

import static io.jooby.ReactiveSupport.concurrent;
import static java.lang.Thread.currentThread;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.mutiny.Mutiny;
import io.jooby.reactor.Reactor;
import io.jooby.rxjava3.Reactivex;
import io.jooby.test.WebClient;
import io.reactivex.rxjava3.core.Single;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import reactor.core.publisher.Mono;

public class Issue2465 {
  @ServerTest
  public void shouldCompletableFutureInvokeAfter(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.before(
                  ctx ->
                      ctx.setAttribute("inc", new AtomicInteger(0)).setResetHeadersOnError(false));

              app.after(
                  (ctx, result, failure) -> {
                    AtomicInteger inc = ctx.getAttribute("inc");
                    if (inc.incrementAndGet() == 1) {
                      if (result != null) {
                        ctx.setResponseHeader("After", result);
                      }
                      if (failure != null) {
                        ctx.setResponseHeader("error", failure.getMessage());
                      }
                      ctx.setResponseHeader("Response-Started", ctx.isResponseStarted());
                    } else {
                      throw new IllegalStateException("After must be called once");
                    }
                  });

              app.use(concurrent());

              app.get(
                  "/2465", ctx -> CompletableFuture.supplyAsync(() -> currentThread().getName()));

              app.get(
                  "/2465/{num}",
                  ctx -> CompletableFuture.supplyAsync(() -> ctx.path("num").intValue()));
            })
        .ready(
            (WebClient http) -> {
              http.get(
                  "/2465",
                  rsp -> {
                    assertEquals(rsp.header("After"), rsp.body().string());
                    assertEquals(rsp.header("Response-Started"), "false");
                  });
              http.get(
                  "/2465/NaN",
                  rsp -> {
                    assertEquals(rsp.header("error"), "Cannot convert value: 'num', to: 'int'");
                    assertEquals(rsp.header("Response-Started"), "false");
                  });
            });
  }

  @ServerTest
  public void shouldMonoInvokeAfter(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.before(
                  ctx ->
                      ctx.setAttribute("inc", new AtomicInteger(0)).setResetHeadersOnError(false));

              app.after(
                  (ctx, result, failure) -> {
                    AtomicInteger inc = ctx.getAttribute("inc");
                    if (inc.incrementAndGet() == 1) {
                      if (result != null) {
                        ctx.setResponseHeader("After", result);
                      }
                      if (failure != null) {
                        ctx.setResponseHeader("error", failure.getMessage());
                      }
                      ctx.setResponseHeader("Response-Started", ctx.isResponseStarted());
                    } else {
                      throw new IllegalStateException("After must be called once");
                    }
                  });

              app.use(Reactor.reactor());

              app.get(
                  "/2465",
                  ctx ->
                      Mono.fromCompletionStage(
                          CompletableFuture.supplyAsync(() -> currentThread().getName())));

              app.get(
                  "/2465/{num}",
                  ctx ->
                      Mono.fromCompletionStage(
                          CompletableFuture.supplyAsync(() -> ctx.path("num").intValue())));
            })
        .ready(
            (WebClient http) -> {
              http.get(
                  "/2465",
                  rsp -> {
                    assertEquals(rsp.header("After"), rsp.body().string());
                    assertEquals(rsp.header("Response-Started"), "false");
                  });
              http.get(
                  "/2465/NaN",
                  rsp -> {
                    assertEquals(rsp.header("error"), "Cannot convert value: 'num', to: 'int'");
                    assertEquals(rsp.header("Response-Started"), "false");
                  });
            });
  }

  @ServerTest
  public void shouldSingleInvokeAfter(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.before(
                  ctx ->
                      ctx.setAttribute("inc", new AtomicInteger(0)).setResetHeadersOnError(false));

              app.after(
                  (ctx, result, failure) -> {
                    AtomicInteger inc = ctx.getAttribute("inc");
                    if (inc.incrementAndGet() == 1) {
                      if (result != null) {
                        ctx.setResponseHeader("After", result);
                      }
                      if (failure != null) {
                        ctx.setResponseHeader("error", failure.getMessage());
                      }
                      ctx.setResponseHeader("Response-Started", ctx.isResponseStarted());
                    } else {
                      throw new IllegalStateException("After must be called once");
                    }
                  });

              app.use(Reactivex.rx());

              app.get(
                  "/2465",
                  ctx ->
                      Single.fromCompletionStage(
                          CompletableFuture.supplyAsync(() -> currentThread().getName())));

              app.get(
                  "/2465/{num}",
                  ctx ->
                      Single.fromCompletionStage(
                          CompletableFuture.supplyAsync(() -> ctx.path("num").intValue())));
            })
        .ready(
            (WebClient http) -> {
              http.get(
                  "/2465",
                  rsp -> {
                    assertEquals(rsp.header("After"), rsp.body().string());
                    assertEquals(rsp.header("Response-Started"), "false");
                  });
              http.get(
                  "/2465/NaN",
                  rsp -> {
                    assertEquals(rsp.header("error"), "Cannot convert value: 'num', to: 'int'");
                    assertEquals(rsp.header("Response-Started"), "false");
                  });
            });
  }

  @ServerTest
  public void shouldUniInvokeAfter(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.before(
                  ctx ->
                      ctx.setAttribute("inc", new AtomicInteger(0)).setResetHeadersOnError(false));

              app.after(
                  (ctx, result, failure) -> {
                    AtomicInteger inc = ctx.getAttribute("inc");
                    if (inc.incrementAndGet() == 1) {
                      if (result != null) {
                        ctx.setResponseHeader("After", result);
                      }
                      if (failure != null) {
                        ctx.setResponseHeader("error", failure.getMessage());
                      }
                      ctx.setResponseHeader("Response-Started", ctx.isResponseStarted());
                    } else {
                      throw new IllegalStateException("After must be called once");
                    }
                  });

              app.use(Mutiny.mutiny());

              app.get(
                  "/2465",
                  ctx ->
                      Uni.createFrom()
                          .completionStage(
                              CompletableFuture.supplyAsync(() -> currentThread().getName())));

              app.get(
                  "/2465/{num}",
                  ctx ->
                      Uni.createFrom()
                          .completionStage(
                              CompletableFuture.supplyAsync(() -> ctx.path("num").intValue())));
            })
        .ready(
            (WebClient http) -> {
              http.get(
                  "/2465",
                  rsp -> {
                    assertEquals(rsp.header("After"), rsp.body().string());
                    assertEquals(rsp.header("Response-Started"), "false");
                  });
              http.get(
                  "/2465/NaN",
                  rsp -> {
                    assertEquals(rsp.header("error"), "Cannot convert value: 'num', to: 'int'");
                    assertEquals(rsp.header("Response-Started"), "false");
                  });
            });
  }

  @ServerTest
  public void shouldMultiInvokeAfter(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.before(
                  ctx ->
                      ctx.setAttribute("inc", new AtomicInteger(0)).setResetHeadersOnError(false));

              app.after(
                  (ctx, result, failure) -> {
                    AtomicInteger inc = ctx.getAttribute("inc");
                    int i = inc.getAndIncrement();
                    if (!ctx.isResponseStarted()) {
                      // Only allowed before sending first element
                      ctx.setResponseHeader("After-" + i, i);
                    }
                    if (failure != null) {
                      ctx.setResponseHeader("error", failure.getMessage());
                    }
                  });

              app.use(Mutiny.mutiny());

              app.get(
                  "/2465",
                  ctx -> Multi.createFrom().range(0, 10).emitOn(ForkJoinPool.commonPool()));
            })
        .ready(
            (WebClient http) -> {
              http.get(
                  "/2465",
                  rsp -> {
                    // First header setup allowed
                    assertEquals("0", rsp.header("After-0"));
                    assertEquals(null, rsp.header("After-1"));
                    assertEquals(null, rsp.header("After-2"));
                    assertEquals(null, rsp.header("After-3"));
                    assertEquals(null, rsp.header("After-4"));
                    assertEquals(null, rsp.header("After-5"));
                    assertEquals(null, rsp.header("After-6"));
                    assertEquals(null, rsp.header("After-7"));
                    assertEquals(null, rsp.header("After-8"));
                    assertEquals(null, rsp.header("After-9"));
                    assertEquals("0123456789", rsp.body().string());
                  });
            });
  }
}
