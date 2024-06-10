/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.completablefuture;

import java.util.concurrent.CompletableFuture;

import io.jooby.annotation.GET;

public class CCompletableFuture {
  @GET("/future")
  public CompletableFuture<String> future() {
    return CompletableFuture.completedFuture("c");
  }
}
