/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3551;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.ServiceKey;
import io.jooby.StatusCode;
import io.jooby.guice.GuiceModule;
import io.jooby.jackson.JacksonModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue3551 {
  @ServerTest
  public void shouldEnsureDIOnStartCallbacks(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.getServices()
                  .put(
                      ServiceKey.key(Service3551.class, "service"),
                      new Service3551(app.getEnvironment()));
              app.install(new JacksonModule().module(JacksonModule3551.class));

              app.install(new GuiceModule());

              app.onStarting(
                  () -> {
                    // Ask jooby registry
                    assertNotNull(app.require(ServiceKey.key(Service3551.class, "service")));
                    // Ask Guice
                    assertNotNull(app.require(GuiceService3551.class));
                    assertNotNull(app.require(JacksonModule3551.class));
                  });
              app.get("/3551", ctx -> StatusCode.OK);
            })
        .ready(
            http -> {
              http.get(
                  "/3551",
                  rsp -> {
                    assertEquals(StatusCode.OK.value(), rsp.code());
                  });
            });
  }
}
