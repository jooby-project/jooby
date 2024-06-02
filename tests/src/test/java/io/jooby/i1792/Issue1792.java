/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i1792;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.StatusCode;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue1792 {
  @ServerTest
  public void scriptMvcRouteShouldProducesSameOutput(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.mvc(new Controller1792_());
              app.post("/s/1792", ctx -> StatusCode.CREATED);
            })
        .ready(
            client -> {
              client.post(
                  "/c/1792",
                  rsp -> {
                    assertEquals("", rsp.body().string());
                    assertEquals(StatusCode.CREATED_CODE, rsp.code());
                  });

              client.post(
                  "/s/1792",
                  rsp -> {
                    assertEquals("", rsp.body().string());
                    assertEquals(StatusCode.CREATED_CODE, rsp.code());
                  });
            });
  }
}
