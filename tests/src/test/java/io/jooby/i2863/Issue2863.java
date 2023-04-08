/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2863;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.Context;
import io.jooby.Cors;
import io.jooby.CorsHandler;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue2863 {

  @ServerTest
  public void corsHandlerShouldNotExecuteHandlerWhenOriginHeaderIsMissing(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              Cors cors = new Cors().setOrigin("*").setMethods("GET", "POST", "PUT", "DELETE");

              app.use(new CorsHandler(cors));

              app.post("/2863", Context::getMethod);
            })
        .ready(
            client -> {
              client.options(
                  "/2863",
                  rsp -> {
                    assertEquals("", rsp.body().string());
                    assertEquals("POST,OPTIONS", rsp.header("Allow"));
                    assertEquals(200, rsp.code());
                  });
            });
  }
}
