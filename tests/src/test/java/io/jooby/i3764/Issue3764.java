/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3764;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.RouterOptions;
import io.jooby.guice.GuiceModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue3764 {
  @ServerTest
  public void shouldGetContextAsService(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.setRouterOptions(new RouterOptions().setContextAsService(true));

              app.install(new GuiceModule());

              app.mvc(new C3764_());
            })
        .ready(
            http -> {
              http.get(
                  "/3764",
                  rsp -> {
                    assertEquals("/3764", rsp.body().string());
                  });
            });
  }
}
