/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i1786;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.StatusCode;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue1786 {
  @ServerTest
  public void shouldFollowNonNullAnnotation(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.mvc(new Controller1786());
            })
        .ready(
            client -> {
              client.get(
                  "/1786/nonnull",
                  rsp -> {
                    assertEquals(StatusCode.BAD_REQUEST_CODE, rsp.code());
                  });
            });
  }
}
