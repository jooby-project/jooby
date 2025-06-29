/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static java.util.stream.LongStream.range;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

import io.jooby.ServerOptions;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import okhttp3.ResponseBody;

public class Issue1656 {

  @ServerTest
  public void gzip(ServerTestRunner runner) {
    runner
        .options(new ServerOptions().setCompressionLevel(ServerOptions.DEFAULT_COMPRESSION_LEVEL))
        .define(
            app -> {
              app.assets("/static/*", "/files");
            })
        .ready(
            client -> {
              client
                  .get("/static/fileupload.js")
                  .prepare(
                      req -> {
                        req.addHeader("Accept-Encoding", "gzip");
                      })
                  .execute(
                      rsp -> {
                        ResponseBody body = rsp.body();
                        long len = body.contentLength();
                        if (len == -1) {
                          assertEquals("chunked", rsp.header("Transfer-Encoding"));
                        } else {
                          assertTrue(
                              range(63, 66).anyMatch(value -> value == rsp.body().contentLength()));
                        }
                        assertEquals("gzip", rsp.header("content-encoding"));
                        assertEquals(
                            "(function () {  console.log('ready');})();",
                            ungzip(body.bytes()).trim());
                      });
            });
  }

  private String ungzip(byte[] buff) throws IOException {
    GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(buff));
    Scanner scanner = new Scanner(gzip);
    StringBuilder str = new StringBuilder();
    while (scanner.hasNext()) {
      str.append(scanner.nextLine());
    }
    return str.toString();
  }
}
