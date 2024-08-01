/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3486;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletableFuture;

import io.jooby.ReactiveSupport;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.mutiny.Mutiny;
import io.jooby.reactor.Reactor;
import io.jooby.rxjava3.Reactivex;
import io.reactivex.rxjava3.core.Single;
import io.smallrye.mutiny.Uni;
import reactor.core.publisher.Mono;

public class Issue3486 {
  @ServerTest
  public void reactiveShouldWorkWithSideEffectsHandler(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.use(ReactiveSupport.concurrent());
              app.use(Mutiny.mutiny());
              app.use(Reactor.reactor());
              app.use(Reactivex.rx());

              app.get(
                  "/3486/completablefuture",
                  ctx ->
                      CompletableFuture.supplyAsync(
                          () -> {
                            ctx.send("top page");
                            return ctx;
                          }));

              app.get(
                  "/3486/mutiny",
                  ctx ->
                      Uni.createFrom()
                          .item(
                              () -> {
                                ctx.send("top page");
                                return ctx;
                              }));

              app.get(
                  "/3486/reactor",
                  ctx ->
                      Mono.fromSupplier(
                          () -> {
                            ctx.send("top page");
                            return ctx;
                          }));

              app.get(
                  "/3486/reactivex",
                  ctx ->
                      Single.fromSupplier(
                          () -> {
                            ctx.send("top page");
                            return ctx;
                          }));
            })
        .ready(
            client -> {
              client.get(
                  "/3486/completablefuture",
                  rsp -> {
                    assertEquals(200, rsp.code());
                    assertEquals("top page", rsp.body().string());
                  });
              client.get(
                  "/3486/mutiny",
                  rsp -> {
                    assertEquals(200, rsp.code());
                    assertEquals("top page", rsp.body().string());
                  });
              client.get(
                  "/3486/reactor",
                  rsp -> {
                    assertEquals(200, rsp.code());
                    assertEquals("top page", rsp.body().string());
                  });
              client.get(
                  "/3486/reactivex",
                  rsp -> {
                    assertEquals(200, rsp.code());
                    assertEquals("top page", rsp.body().string());
                  });
            });
  }
}
