/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.jackson.JacksonModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class Issue1391 {

  @ServerTest
  public void issue1391(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.install(new JacksonModule());
              app.mvc(new Controller1391());
            })
        .ready(
            client -> {
              client.post(
                  "/1391",
                  RequestBody.create("[{\"name\" : \"1392\"}]", MediaType.get("application/json")),
                  rsp -> {
                    assertEquals("[{\"name\":\"1392\"}]", rsp.body().string());
                  });
            });
  }
}
