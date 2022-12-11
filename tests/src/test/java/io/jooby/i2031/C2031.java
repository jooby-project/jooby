/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2031;

import java.util.concurrent.CompletableFuture;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;
import io.reactivex.rxjava3.core.Single;
import io.smallrye.mutiny.Uni;
import reactor.core.publisher.Mono;

@Path("/i2031")
public class C2031 {

  @GET("/completableFuture")
  public CompletableFuture<String> completableFuture() {
    return CompletableFuture.supplyAsync(() -> CompletableFuture.class.getSimpleName())
        .thenApply(value -> "Hello " + value);
  }

  @GET("/single")
  public Single<String> single() {
    return Single.fromCompletionStage(
            CompletableFuture.supplyAsync(() -> Single.class.getSimpleName()))
        .map(value -> "Hello " + value);
  }

  @GET("/mono")
  public Mono<String> mono() {
    return Mono.fromCompletionStage(CompletableFuture.supplyAsync(() -> Mono.class.getSimpleName()))
        .map(value -> "Hello " + value);
  }

  @GET("/uni")
  public Uni<String> uni() {
    return Uni.createFrom()
        .completionStage(CompletableFuture.supplyAsync(() -> Uni.class.getSimpleName()))
        .map(value -> "Hello " + value);
  }
}
