/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3501;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.handler.AssetHandler;
import io.jooby.handler.AssetSource;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue3501 {

  @ServerTest
  public void assetHandlerShouldGenerateCustom404Response(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.assets(
                  "/issue3501/*",
                  new AssetHandler(AssetSource.create(getClass().getClassLoader(), "/static"))
                      .notFound(
                          ctx -> {
                            throw new UnsupportedOperationException();
                          }));

              app.error(
                  UnsupportedOperationException.class,
                  ((ctx, cause, code) -> {
                    ctx.send(cause.getClass().getName());
                  }));
            })
        .ready(
            http -> {
              http.get(
                  "/issue3501/index.js",
                  rsp -> {
                    assertEquals(
                        UnsupportedOperationException.class.getName(), rsp.body().string());
                    assertEquals(500, rsp.code());
                  });
            });
  }
}
