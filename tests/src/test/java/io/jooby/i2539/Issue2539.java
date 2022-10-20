/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2539;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.Jooby;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue2539 {

  static class AppA extends Jooby {
    {
      get(
          "/throwsGeneric",
          ctx -> {
            throw new IllegalStateException(ctx.getRequestPath());
          });

      get(
          "/throwsA",
          ctx -> {
            throw new ExceptionA();
          });

      error(
          ExceptionA.class,
          (ctx, cause, code) -> {
            ctx.send("exception A was thrown");
          });
    }
  }

  static class AppB extends Jooby {
    {
      get(
          "/throwsB",
          ctx -> {
            throw new ExceptionB();
          });

      // THIS DOES NOT WORK!!!!
      error(
          ExceptionB.class,
          (ctx, cause, code) -> {
            ctx.send("exception B was thrown");
          });
    }
  }

  static class ExceptionA extends RuntimeException {}

  static class ExceptionB extends RuntimeException {}

  static class ExceptionRoot extends RuntimeException {}

  @ServerTest
  public void shouldErrorHandlerWorkForMountedResources(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.get(
                  "/2539",
                  ctx -> {
                    throw new ExceptionRoot();
                  });

              app.mount(new AppA());
              app.mount(new AppB());

              app.error(
                  ExceptionRoot.class,
                  (ctx, cause, code) -> {
                    ctx.send("exception Root was thrown");
                  });

              app.error(
                  (ctx, cause, code) -> {
                    ctx.send("exception was thrown");
                  });
            })
        .ready(
            http -> {
              http.get(
                  "/2539",
                  rsp -> {
                    assertEquals("exception Root was thrown", rsp.body().string());
                  });

              http.get(
                  "/throwsGeneric",
                  rsp -> {
                    assertEquals("exception was thrown", rsp.body().string());
                  });

              http.get(
                  "/throwsA",
                  rsp -> {
                    assertEquals("exception A was thrown", rsp.body().string());
                  });

              http.get(
                  "/throwsB",
                  rsp -> {
                    assertEquals("exception B was thrown", rsp.body().string());
                  });
            });
  }
}
