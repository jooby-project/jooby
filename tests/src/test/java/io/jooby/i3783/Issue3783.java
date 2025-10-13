/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3783;

import static org.junit.jupiter.api.Assertions.*;

import io.jooby.ServerOptions;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import okhttp3.FormBody;

public class Issue3783 {

  @ServerTest
  public void shouldAllowToSetMaxFormFields(ServerTestRunner runner) {
    runner
        .options(new ServerOptions().setMaxFormFields(2))
        .define(
            app -> {
              app.error((ctx, cause, code) -> ctx.send(cause.getMessage()));
              app.post(
                  "/3723",
                  ctx -> {
                    return ctx.form("f").toList();
                  });
            })
        .ready(
            http -> {
              http.post(
                  "/3723",
                  new FormBody.Builder()
                      .add("f1", "value 1")
                      .add("f2", "value 2")
                      .add("f3", "value 3")
                      .build(),
                  rsp -> {
                    assertEquals(400, rsp.code());
                    assertEquals("Too many form fields", rsp.body().string());
                  });
            });
  }
}
