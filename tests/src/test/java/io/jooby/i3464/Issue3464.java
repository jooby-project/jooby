/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3464;

import static io.jooby.test.TestUtil._19kb;
import static okhttp3.RequestBody.create;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import okhttp3.MediaType;
import okhttp3.MultipartBody;

public class Issue3464 {

  @ServerTest
  public void shouldSupportLargeTextFileWithoutConvertingToFiles(ServerTestRunner runner) {
    var large = _19kb.repeat(10);
    runner
        .define(
            app -> {
              app.post(
                  "/3464",
                  ctx -> {
                    assertTrue(ctx.form().files().isEmpty());
                    return ctx.form("large").value();
                  });
              app.post(
                  "/3464/files",
                  ctx -> {
                    assertFalse(ctx.form().files().isEmpty());
                    try (var upload = ctx.file("large")) {
                      return new String(upload.bytes(), StandardCharsets.UTF_8);
                    }
                  });
            })
        .ready(
            http -> {
              http.post(
                  "/3464",
                  new MultipartBody.Builder()
                      .setType(MultipartBody.FORM)
                      .addFormDataPart("large", large)
                      .build(),
                  rsp -> {
                    assertEquals(large, rsp.body().string());
                  });
              http.post(
                  "/3464/files",
                  new MultipartBody.Builder()
                      .setType(MultipartBody.FORM)
                      .addFormDataPart(
                          "large", "large.txt", create(large, MediaType.parse("text/plain")))
                      .build(),
                  rsp -> {
                    assertEquals(large, rsp.body().string());
                  });
            });
  }
}
