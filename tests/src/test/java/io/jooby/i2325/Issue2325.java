/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2325;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue2325 {
  @ServerTest
  public void shouldNamedParamWorkWithCustomValueConverter(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              var factory = app.getValueFactory();
              factory.put(MyID2325.class, new VC2325());

              app.mvc(new C2325_());
            })
        .ready(
            http -> {
              UUID uuid = UUID.randomUUID();
              // With custom converter
              http.get(
                  "/2325?id=" + uuid,
                  rsp -> {
                    assertEquals("MyID:" + uuid, rsp.body().string());
                  });
              // With bean converter
              http.get(
                  "/2325?value=" + uuid,
                  rsp -> {
                    assertEquals("MyID:" + uuid, rsp.body().string());
                  });
            });
  }

  @ServerTest
  public void shouldWorkWithDefaultCustomConverter(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.mvc(new C2325_());
              app.error(
                  (ctx, cause, code) -> {
                    ctx.send(cause.getMessage());
                  });
            })
        .ready(
            http -> {
              UUID uuid = UUID.randomUUID();
              // Fail don't know what to do with: id
              http.get(
                  "/2325?id=" + uuid,
                  rsp -> {
                    assertEquals("MyID:" + uuid, rsp.body().string());
                  });
            });
  }

  @ServerTest
  public void shouldConvertUsingBeanConverter(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.mvc(new C2325_());
            })
        .ready(
            http -> {
              UUID uuid = UUID.randomUUID();
              // With bean converter
              http.get(
                  "/2325?value=" + uuid,
                  rsp -> {
                    assertEquals("MyID:" + uuid, rsp.body().string());
                  });
            });
  }
}
