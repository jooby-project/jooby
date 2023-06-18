/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.jooby.handler.Cors;
import io.jooby.handler.CorsHandler;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue2649 {

  @ServerTest
  public void shouldDoPreflightWithout415(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.use(new CorsHandler(new Cors()));

              app.post("/consumes", Context::getMethod).consumes(MediaType.json);
              app.post("/produces", Context::getMethod).produces(MediaType.json);
            })
        .ready(
            client -> {
              // OPTIONS (Pre-flight), checking GET Method => OK and Access Control Headers Present
              client
                  .header("Access-Control-Request-Method", "POST")
                  .options(
                      "/consumes",
                      rsp -> {
                        assertEquals("", rsp.body().string());
                        assertEquals("POST,OPTIONS", rsp.header("Allow"));
                        assertEquals(200, rsp.code());
                      });

              client
                  .header("Access-Control-Request-Method", "POST")
                  .options(
                      "/produces",
                      rsp -> {
                        assertEquals("", rsp.body().string());
                        assertEquals("POST,OPTIONS", rsp.header("Allow"));
                        assertEquals(200, rsp.code());
                      });

              // Execute handler (not preflight)
              client
                  .header("Origin", "https://foo.org")
                  .post(
                      "/consumes",
                      rsp -> {
                        // headers are reset on error
                        assertNull(rsp.header("Access-Control-Allow-Origin"));
                        assertEquals(415, rsp.code());
                      });

              client.post(
                  "/consumes",
                  rsp -> {
                    assertEquals(415, rsp.code());
                    assertNull(rsp.header("Access-Control-Allow-Origin"));
                  });

              client
                  .header("Origin", "https://foo.org")
                  .header("Content-Type", "application/json")
                  .post(
                      "/consumes",
                      rsp -> {
                        assertEquals("POST", rsp.body().string());
                        assertEquals("https://foo.org", rsp.header("Access-Control-Allow-Origin"));
                        assertEquals(200, rsp.code());
                      });

              client
                  .header("Content-Type", "application/json")
                  .post(
                      "/consumes",
                      rsp -> {
                        assertEquals("POST", rsp.body().string());
                        assertNull(rsp.header("Access-Control-Allow-Origin"));
                        assertEquals(200, rsp.code());
                      });
            });
  }
}
