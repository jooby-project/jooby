package io.jooby;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import okhttp3.MediaType;

import static okhttp3.RequestBody.create;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1548 {

  @ServerTest
  public void issue1548(ServerTestRunner runner) {
    runner.define(app -> {
      app.post("/1548", ctx -> ctx.multipart().toString());
    }).ready(client -> {
      client
          .post("/1548", create("{\"foo\": \"bar\"}", MediaType.parse("application/json")), rsp -> {
            assertEquals("{}", rsp.body().string());
          });
    });
  }
}
