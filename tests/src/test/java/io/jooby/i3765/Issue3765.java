/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3765;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;

import io.jooby.FileDownload;
import io.jooby.SneakyThrows;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.test.TestUtil;

public class Issue3765 {
  @ServerTest
  public void shouldDestroyFileAfterSend(ServerTestRunner runner) throws IOException {
    var path = getPath(runner.getServer().toLowerCase());
    // Create file
    Files.writeString(path, TestUtil._19kb + TestUtil._19kb + TestUtil._19kb);
    runner
        .define(
            app -> {
              app.get("/3765", ctx -> FileDownload.build(path).deleteOnComplete().attachment());
            })
        .ready(
            http -> {
              http.get(
                  "/3765",
                  rsp -> {
                    assertEquals(200, rsp.code());
                    assertEquals("56832", rsp.header("Content-Length"));
                  });
              while (Files.exists(path)) {
                Thread.sleep(10);
              }
              assertFalse(Files.exists(path), "File " + path + " exists");
            });
  }

  @AfterAll
  public static void filesWhereDeleted() {
    Stream.of("jetty", "netty", "undertow")
        .map(Issue3765::getPath)
        .forEach(
            SneakyThrows.throwingConsumer(
                path -> assertFalse(Files.exists(path), "File " + path + " exists")));
  }

  private static Path getPath(String suffix) {
    return Paths.get(System.getProperty("java.io.tmpdir"), "i3765-" + suffix + ".txt");
  }
}
