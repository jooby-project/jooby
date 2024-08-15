/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3400;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.Jooby;
import io.jooby.jackson.JacksonModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class Issue3400 {

  static class AppA extends Jooby {
    {
      post("/pets", ctx -> ctx.body(Pet3400.class));
    }
  }

  @ServerTest
  public void shouldShareDecodersOnMountedResources(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.install(new JacksonModule());
              app.mount(new AppA());
            })
        .ready(
            http -> {
              http.post(
                  "/pets",
                  RequestBody.create(
                      "{\"id\": 1, \"name\": \"Cheddar\"}", MediaType.parse("application/json")),
                  rsp -> {
                    assertEquals("{\"id\":1,\"name\":\"Cheddar\"}", rsp.body().string());
                    assertEquals("application/json;charset=UTF-8", rsp.header("Content-Type"));
                  });
            });
  }

  @ServerTest
  public void shouldShareDecodersOnInstalledResources(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.install(new JacksonModule());
              app.install(AppA::new);
            })
        .ready(
            http -> {
              http.post(
                  "/pets",
                  RequestBody.create(
                      "{\"id\": 1, \"name\": \"Cheddar\"}", MediaType.parse("application/json")),
                  rsp -> {
                    assertEquals("{\"id\":1,\"name\":\"Cheddar\"}", rsp.body().string());
                    assertEquals("application/json;charset=UTF-8", rsp.header("Content-Type"));
                  });
            });
  }
}
