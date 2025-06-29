/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2363;

import static okhttp3.RequestBody.create;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.jooby.ServerOptions;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import okhttp3.MediaType;
import okhttp3.MultipartBody;

public class Issue2363 {

  /**
   * Test for https://github.com/jooby-project/jooby/issues/2363.
   *
   * <p>We just make sure it works as expected but I can't figure it out how to test/assert for a
   * 100 response using OKHttpClient.
   *
   * @param runner Test runner.
   */
  @ServerTest
  public void shouldAllowExpectAndContinue(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.post("/2363", ctx -> new String(ctx.file("f").bytes(), StandardCharsets.UTF_8));
            })
        .options(new ServerOptions().setExpectContinue(true))
        .ready(
            http -> {
              http.header("Expect", "100-continue")
                  .post(
                      "/2363",
                      new MultipartBody.Builder()
                          .setType(MultipartBody.FORM)
                          .addFormDataPart(
                              "f",
                              "fileupload.js",
                              create(
                                  userdir("src", "test", "resources", "files", "fileupload.js")
                                      .toFile(),
                                  MediaType.parse("application/javascript")))
                          .build(),
                      rsp -> {
                        assertEquals(200, rsp.code());
                        assertEquals(
                            new String(
                                Files.readAllBytes(
                                    userdir("src", "test", "resources", "files", "fileupload.js")),
                                StandardCharsets.UTF_8),
                            rsp.body().string());
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
