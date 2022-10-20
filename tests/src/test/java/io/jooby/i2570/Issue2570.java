/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2570;

import static io.jooby.test.TestSupport.userdir;
import static okhttp3.RequestBody.create;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import okhttp3.MediaType;
import okhttp3.MultipartBody;

public class Issue2570 {
  @ServerTest
  public void shouldSetFileOnBean(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.mvc(new Controller2570());
            })
        .ready(
            http -> {
              http.post(
                  "/2570",
                  new MultipartBody.Builder()
                      .setType(MultipartBody.FORM)
                      .addFormDataPart("name", "file name")
                      .addFormDataPart(
                          "file",
                          "fileupload.js",
                          create(
                              userdir("src", "test", "resources", "files", "fileupload.js")
                                  .toFile(),
                              MediaType.parse("application/javascript")))
                      .build(),
                  rsp -> {
                    assertEquals("file name: fileupload.js", rsp.body().string());
                  });
            });
  }
}
