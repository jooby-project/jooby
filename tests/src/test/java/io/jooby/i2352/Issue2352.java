/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2352;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import okhttp3.MultipartBody;

public class Issue2352 {
  @ServerTest
  public void shouldNotIgnoreAnnotationOnParam(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.mvc(new C2352_());
              app.error(
                  (ctx, cause, code) -> {
                    ctx.send(cause.getMessage());
                  });
            })
        .ready(
            http -> {
              String name = getClass().getSimpleName();
              http.post(
                  "/2352/nonnull",
                  new MultipartBody.Builder()
                      .setType(MultipartBody.FORM)
                      .addFormDataPart("noname", name)
                      .build(),
                  rsp -> {
                    assertEquals(400, rsp.code());
                    assertEquals("Missing value: 'name'", rsp.body().string());
                  });

              http.post(
                  "/2352/nonnull",
                  new MultipartBody.Builder()
                      .setType(MultipartBody.FORM)
                      .addFormDataPart("name", name)
                      .build(),
                  rsp -> {
                    assertEquals(name, rsp.body().string());
                  });

              http.post(
                  "/2352/nullable",
                  new MultipartBody.Builder()
                      .setType(MultipartBody.FORM)
                      .addFormDataPart("noname", name)
                      .build(),
                  rsp -> {
                    assertEquals(200, rsp.code());
                    assertEquals("null", rsp.body().string());
                  });
            });
  }
}
