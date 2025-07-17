/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3721;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import io.jooby.ServerOptions;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue3721 {

  @ServerTest
  public void shouldAllowToSetMaxHeaderSize(ServerTestRunner runner) {
    runner
        .options(new ServerOptions().setMaxHeaderSize(ServerOptions._16KB))
        .define(
            app -> {
              app.get(
                  "/3721",
                  ctx -> {
                    return ctx.header("large").value();
                  });
            })
        .ready(
            http -> {
              var large = ".".repeat(ServerOptions._8KB + ServerOptions._4KB);
              http.header("large", large);
              http.get(
                  "/3721",
                  rsp -> {
                    var result = rsp.body().string();
                    assertEquals(large, result);
                    assertEquals(ServerOptions._8KB + ServerOptions._4KB, result.length());
                  });
            });
  }

  @ServerTest
  public void shouldCheckErrorOnLargeHeaderSize(ServerTestRunner runner) {
    runner
        .options(new ServerOptions().setMaxHeaderSize(ServerOptions._4KB))
        .define(
            app -> {
              app.get(
                  "/3721",
                  ctx -> {
                    return ctx.header("large").value();
                  });
            })
        .ready(
            http -> {
              var large = ".".repeat(ServerOptions._8KB);
              http.header("large", large);
              http.get(
                  "/3721",
                  rsp -> {
                    assertTrue(Set.of(400, 431).contains(rsp.code()));
                  });
            });
  }
}
