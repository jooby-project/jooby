/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import io.jooby.SSLHandler;
import io.jooby.ServerOptions;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class Issue2372 {

  @ServerTest
  public void http2(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.setServerOptions(new ServerOptions().setHttp2(true).setSecurePort(8443));

              app.before(new SSLHandler());

              app.get(
                  "/2372/mono",
                  ctx -> {
                    return Mono.fromCallable(() -> "Welcome to Jooby!");
                  });

              app.get(
                  "/2372/flux",
                  ctx -> {
                    return Flux.fromIterable(Arrays.asList("Welcome", "to", "Jooby!"))
                        .map(it -> it + " ");
                  });
            })
        .ready(
            (http, https) -> {
              https.get(
                  "/2372/flux",
                  rsp -> {
                    assertEquals("Welcome to Jooby!", rsp.body().string().trim());
                  });
              https.get(
                  "/2372/mono",
                  rsp -> {
                    assertEquals("Welcome to Jooby!", rsp.body().string());
                  });
            });
  }
}
