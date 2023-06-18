/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static okhttp3.RequestBody.create;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;

import io.jooby.StatusCode;
import io.jooby.handler.CsrfHandler;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import okhttp3.MediaType;
import okhttp3.MultipartBody;

public class Issue1815 {

  @ServerTest
  public void shouldNotThrowClassCastExceptionOnFileUploadWithCsrfHandler(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.before(new CsrfHandler());

              app.post("/1815", ctx -> ctx.form("file"));
            })
        .ready(
            http -> {
              http.post(
                  "/1815",
                  new MultipartBody.Builder()
                      .setType(MultipartBody.FORM)
                      .addFormDataPart(
                          "file",
                          "fileupload.js",
                          create(
                              userdir("src", "test", "resources", "files", "fileupload.js")
                                  .toFile(),
                              MediaType.parse("application/javascript")))
                      .build(),
                  rsp -> {
                    assertEquals(StatusCode.FORBIDDEN_CODE, rsp.code());
                  });
            });
  }

  private static Path userdir(String... segments) {
    Path path = Paths.get(System.getProperty("user.dir"));
    for (String segment : segments) {
      path = path.resolve(segment);
    }
    return path;
  }
}
