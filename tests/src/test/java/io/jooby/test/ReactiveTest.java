/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static reactor.core.scheduler.Schedulers.parallel;

import java.io.Writer;

import io.jooby.ReactiveSupport;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.mutiny.Mutiny;
import io.jooby.reactor.Reactor;
import io.jooby.rxjava3.Reactivex;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ReactiveTest {
  @ServerTest
  public void rx3(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.use(Reactivex.rx());
              app.get(
                  "/rx/flowable",
                  ctx ->
                      Flowable.range(1, 10)
                          .map(i -> i + ",")
                          .subscribeOn(Schedulers.io())
                          .observeOn(Schedulers.computation()));
              app.get(
                  "/rx/observable",
                  ctx ->
                      Observable.range(1, 10)
                          .map(i -> i + ",")
                          .subscribeOn(Schedulers.io())
                          .observeOn(Schedulers.computation()));
              app.get(
                  "/rx/single",
                  ctx ->
                      Single.fromCallable(() -> "Single")
                          .map(s -> "Hello " + s)
                          .subscribeOn(Schedulers.io())
                          .observeOn(Schedulers.computation()));
              app.get(
                  "/rx/maybe",
                  ctx ->
                      Maybe.fromCallable(() -> "Maybe")
                          .map(s -> "Hello " + s)
                          .subscribeOn(Schedulers.io())
                          .observeOn(Schedulers.computation()));

              app.get("/rx/nomaybe", ctx -> Maybe.empty().subscribeOn(Schedulers.io()));

              app.get(
                  "/rx/flowable/each",
                  ctx -> {
                    Writer writer = ctx.responseWriter();
                    return Flowable.range(1, 10)
                        .map(i -> i + ",")
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.computation())
                        .doOnError(ctx::sendError)
                        .doFinally(writer::close)
                        .forEach(
                            it -> {
                              writer.write(it);
                            });
                  });
            })
        .ready(
            client -> {
              client.get(
                  "/rx/flowable",
                  rsp -> {
                    assertEquals("chunked", rsp.header("transfer-encoding").toLowerCase());
                    assertEquals("1,2,3,4,5,6,7,8,9,10,", rsp.body().string());
                  });
              client.get(
                  "/rx/observable",
                  rsp -> {
                    assertEquals("chunked", rsp.header("transfer-encoding").toLowerCase());
                    assertEquals("1,2,3,4,5,6,7,8,9,10,", rsp.body().string());
                  });
              client.get(
                  "/rx/single",
                  rsp -> {
                    assertEquals("Hello Single", rsp.body().string());
                  });
              client.get(
                  "/rx/maybe",
                  rsp -> {
                    assertEquals("Hello Maybe", rsp.body().string());
                  });
              client.get(
                  "/rx/nomaybe",
                  rsp -> {
                    assertEquals(404, rsp.code());
                    assertEquals("", rsp.body().string());
                  });
              client.get(
                  "/rx/flowable/each",
                  rsp -> {
                    assertEquals("chunked", rsp.header("transfer-encoding").toLowerCase());
                    assertEquals("1,2,3,4,5,6,7,8,9,10,", rsp.body().string());
                  });
            });
  }

  @ServerTest
  public void reactor(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.use(Reactor.reactor());
              app.get(
                  "/reactor/mono",
                  ctx ->
                      Mono.fromCallable(() -> "Mono")
                          .map(s -> "Hello " + s)
                          .subscribeOn(parallel()));
              app.get(
                  "/reactor/flux",
                  ctx -> Flux.range(1, 10).map(i -> i + ",").subscribeOn(parallel()));
            })
        .ready(
            client -> {
              client.get(
                  "/reactor/mono",
                  rsp -> {
                    assertEquals("10", rsp.header("content-length").toLowerCase());
                    assertEquals("Hello Mono", rsp.body().string());
                  });
              client.get(
                  "/reactor/flux",
                  rsp -> {
                    assertEquals("chunked", rsp.header("transfer-encoding").toLowerCase());
                    assertEquals("1,2,3,4,5,6,7,8,9,10,", rsp.body().string());
                  });
            });
  }

  @ServerTest
  public void mutiny(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.use(Mutiny.mutiny());
              app.get(
                  "/mutiny/uni",
                  ctx ->
                      Uni.createFrom()
                          .completionStage(supplyAsync(() -> "Uni"))
                          .map(s -> "Hello " + s));
              app.get("/mutiny/multi", ctx -> Multi.createFrom().range(1, 11).map(i -> i + ","));
            })
        .ready(
            client -> {
              client.get(
                  "/mutiny/uni",
                  rsp -> {
                    assertEquals("9", rsp.header("content-length").toLowerCase());
                    assertEquals("Hello Uni", rsp.body().string());
                  });
              client.get(
                  "/mutiny/multi",
                  rsp -> {
                    assertEquals("chunked", rsp.header("transfer-encoding").toLowerCase());
                    assertEquals("1,2,3,4,5,6,7,8,9,10,", rsp.body().string());
                  });
            });
  }

  @ServerTest
  public void flow(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.use(ReactiveSupport.concurrent());
              app.get("/mutiny/multi", ctx -> Multi.createFrom().range(1, 11).map(i -> i + ","));
            })
        .ready(
            client -> {
              client.get(
                  "/mutiny/multi",
                  rsp -> {
                    assertEquals("chunked", rsp.header("transfer-encoding").toLowerCase());
                    assertEquals("1,2,3,4,5,6,7,8,9,10,", rsp.body().string());
                  });
            });
  }

  @ServerTest
  public void completableFuture(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.use(ReactiveSupport.concurrent());
              app.get(
                  "/completable",
                  ctx -> supplyAsync(() -> "Completable Future!").thenApply(v -> "Hello " + v));
            })
        .ready(
            client -> {
              client.get(
                  "/completable",
                  rsp -> {
                    assertEquals("Hello Completable Future!", rsp.body().string());
                  });
            });
  }
}
