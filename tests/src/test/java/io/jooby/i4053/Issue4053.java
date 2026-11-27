/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i4053;

import static io.jooby.test.TestUtil._16kb;
import static io.jooby.test.TestUtil._19kb;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import io.jooby.ServerOptions;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

public class Issue4053 {

  @ServerTest
  public void maxRequestSizeAppliesToChunkedBody(ServerTestRunner runner) {
    runner
        .options(new ServerOptions().setMaxRequestSize(ServerOptions._16KB))
        .define(app -> app.post("/4053", ctx -> ctx.body().value("")))
        .ready(
            client -> {
              client.post(
                  "/4053",
                  chunked(_19kb),
                  rsp -> {
                    assertEquals(413, rsp.code());
                  });
              client.post(
                  "/4053",
                  chunked(_16kb),
                  rsp -> {
                    assertEquals(200, rsp.code());
                    assertEquals(_16kb, rsp.body().string());
                  });
            });
  }

  // OkHttp sends a body of unknown length with Transfer-Encoding: chunked.
  private static RequestBody chunked(String value) {
    return new RequestBody() {
      @Override
      public MediaType contentType() {
        return MediaType.get("text/plain");
      }

      @Override
      public long contentLength() {
        return -1;
      }

      @Override
      public void writeTo(BufferedSink sink) throws IOException {
        sink.writeUtf8(value);
      }
    };
  }
}
