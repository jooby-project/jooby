/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.trpc.i3863;

import java.util.concurrent.CompletableFuture;

import io.jooby.annotation.*;
import reactor.core.publisher.Mono;

@Path("/users")
@Trpc("users") // Custom namespace
public class C3863 {

  @GET("/{id}")
  @Trpc
  public U3863 getUser(String id) {
    return new U3863(id, "user");
  }

  @POST
  @Trpc
  public U3863 createUser(U3863 user) {
    return user;
  }

  @PUT
  @Trpc
  public CompletableFuture<U3863> createFuture(U3863 user) {
    return CompletableFuture.completedFuture(user);
  }

  @Trpc.Mutation
  public Mono<U3863> createMono(U3863 user) {
    return Mono.just(user);
  }

  @GET("/internal")
  public String internalEndpoint() {
    return "This should not be exposed to tRPC";
  }
}
