/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import com.typesafe.config.Config;
import io.jooby.Environment;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue1774 {

  @ServerTest
  public void shouldHaveAccessToConf(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.get(
                  "/",
                  ctx ->
                      Optional.of(ctx.require(Environment.class)).map(e -> "env;").orElse("")
                          + Optional.of(ctx.require(Config.class)).map(c -> "conf").orElse(""));
            })
        .ready(
            http -> {
              http.get(
                  "/",
                  rsp -> {
                    assertEquals("env;conf", rsp.body().string());
                  });
            });
  }
}
