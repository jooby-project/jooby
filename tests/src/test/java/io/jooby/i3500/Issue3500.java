/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3500;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class Issue3500 {

  @ServerTest
  public void shouldShareDecodersOnMountedResources(ServerTestRunner runner) {
    runner
        .use(WidgetService::new)
        .ready(
            http -> {
              http.post(
                  "/api/widgets1",
                  RequestBody.create("{\"id\": 1}", MediaType.get("application/json")),
                  rsp -> {
                    assertEquals(201, rsp.code());
                  });
              http.post(
                  "/api/widgets2",
                  RequestBody.create("{\"id\": 1}", MediaType.get("application/json")),
                  rsp -> {
                    assertEquals(201, rsp.code());
                  });
            });
  }
}
