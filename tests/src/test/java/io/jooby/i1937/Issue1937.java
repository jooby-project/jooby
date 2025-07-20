/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i1937;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jooby.Context;
import io.jooby.exception.RegistryException;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue1937 {

  @ServerTest
  public void shouldWorkIfContextAsServiceWasCalled(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.get(
                  "/i1937",
                  ctx -> {
                    app.require(Context.class);
                    return "OK";
                  });
            })
        .ready(http -> http.get("/i1937", rsp -> assertEquals(200, rsp.code())));
  }

  @ServerTest
  public void shouldThrowIfOutOfScope(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.onStarted(
                  () -> {
                    Throwable t =
                        assertThrows(RegistryException.class, () -> app.require(Context.class));
                    assertEquals(
                        t.getMessage(),
                        "Context is not available. Are you getting it from request scope?");
                  });
            })
        .ready(http -> {});
  }
}
