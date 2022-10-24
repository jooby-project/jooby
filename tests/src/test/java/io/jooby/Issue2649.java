/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue2649 {

  @ServerTest
  public void shouldDoPreflightWithout415(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.use(new CorsHandler(new Cors()));

              app.post("/consumes", Context::getRequestPath).consumes(MediaType.json);
              app.post("/produces", Context::getRequestPath).produces(MediaType.json);
            })
        .ready(
            client -> {
              // OPTIONS (Pre-flight), checking GET Method => OK and Access Control Headers Present
              client
                  .header("Access-Control-Request-Method", "POST")
                  .options(
                      "/consumes",
                      rsp -> {
                        assertEquals("/consumes", rsp.body().string());
                        assertEquals(200, rsp.code());
                      });

              client
                  .header("Access-Control-Request-Method", "POST")
                  .options(
                      "/produces",
                      rsp -> {
                        assertEquals("/produces", rsp.body().string());
                        assertEquals(200, rsp.code());
                      });
            });
  }
}
