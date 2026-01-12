/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3840;

import java.util.concurrent.CompletableFuture;

import io.jooby.annotation.GET;
import io.jooby.annotation.Path;

@Path("/3840")
public class C3840 {

  @GET
  public CompletableFuture<String> hello() {
    return CompletableFuture.supplyAsync(() -> "hello")
        .whenComplete((e, o) -> o.printStackTrace()); // <- root cause line
  }
}
