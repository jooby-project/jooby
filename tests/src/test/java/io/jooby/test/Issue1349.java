/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jooby.Jooby;
import io.jooby.StatusCode;
import io.jooby.exception.StatusCodeException;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue1349 {

  public static class App1349 extends Jooby {
    {
      get("/1349", ctx -> something());
      get("/1349/iae", ctx -> throwsIAE());
    }

    private String throwsIAE() throws IllegalAccessException {
      throw new IllegalAccessException("no-access");
    }

    public String something() {
      throw new StatusCodeException(StatusCode.UNAUTHORIZED, "test");
    }
  }

  @ServerTest
  public void issue1349(ServerTestRunner runner) {
    runner
        .use(App1349::new)
        .ready(
            client -> {
              client.get(
                  "/1349",
                  rsp -> {
                    assertEquals(401, rsp.code());
                  });
              client.get(
                  "/1349/iae",
                  rsp -> {
                    assertEquals(500, rsp.code());
                    assertTrue(rsp.body().string().contains("message: no-access"));
                  });
            });
  }
}
