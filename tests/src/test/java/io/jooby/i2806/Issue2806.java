/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2806;

import static io.jooby.test.TestUtil._19kb;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import io.jooby.ServerOptions;
import io.jooby.jackson.JacksonModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.output.OutputOptions;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class Issue2806 {

  @ServerTest
  public void renderShouldWorkFromErrorHandlerWhenLargeRequestAreSent(ServerTestRunner runner) {
    runner
        .options(
            new ServerOptions()
                .setOutput(new OutputOptions().setSize(ServerOptions._16KB / 2))
                .setMaxRequestSize(ServerOptions._16KB))
        .define(
            app -> {
              app.install(new JacksonModule());

              app.error(
                  (ctx, cause, code) -> {
                    Map map =
                        ImmutableMap.of(
                            "router", ctx.getRouter() != null, "route", ctx.getRoute() != null);
                    ctx.render(map);
                  });

              app.post(
                  "/2806",
                  ctx -> {
                    return ctx.body().value("");
                  });

              app.get("/2806", ctx -> ctx.body().value(""));
            })
        .ready(
            client -> {
              // Exceeds
              client
                  .post("/2806", RequestBody.create(_19kb, MediaType.get("text/plain")))
                  .execute(
                      rsp -> {
                        assertEquals(413, rsp.code());
                        assertEquals("{\"router\":true,\"route\":true}", rsp.body().string());
                      });

              client
                  .get("/2806")
                  .execute(
                      rsp -> {
                        assertEquals(200, rsp.code());
                        assertEquals("", rsp.body().string());
                      });
            });
  }
}
