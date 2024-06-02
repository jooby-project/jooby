/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.i1573.Controller1573_;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue1573 {

  @ServerTest
  public void issue1573(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.get("/edit/{id:[0-9]+}?", ctx -> ctx.path("id").value("own"));
              app.mvc(new Controller1573_());
            })
        .ready(
            client -> {
              client.get(
                  "/edit",
                  rsp -> {
                    assertEquals("own", rsp.body().string());
                  });

              client.get(
                  "/edit/123",
                  rsp -> {
                    assertEquals("123", rsp.body().string());
                  });

              client.get(
                  "/profile",
                  rsp -> {
                    assertEquals("self", rsp.body().string());
                  });

              client.get(
                  "/profile/123",
                  rsp -> {
                    assertEquals("123", rsp.body().string());
                  });
            });
  }
}
