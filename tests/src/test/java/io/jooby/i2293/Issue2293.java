/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2293;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import io.jooby.SneakyThrows;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import reactor.core.publisher.Mono;

public class Issue2293 {

  @ServerTest
  public void shouldSendLargeResponseAsNonBlocking(ServerTestRunner runner) {
    String resource = "i2293.json";
    runner
        .define(
            app -> {
              app.get(
                  "/2293", ctx -> Mono.fromCallable(() -> resource).map(this::readJsonAsString));
            })
        .ready(
            http -> {
              http.get(
                  "/2293",
                  rsp -> {
                    assertEquals(readJsonAsString(resource), rsp.body().string());
                  });
            });
  }

  public String readJsonAsString(String resourcePath) {
    try {
      String result = null;
      InputStream is = Issue2293.class.getResourceAsStream(resourcePath);
      if (is != null) {
        result = IOUtils.toString(is, StandardCharsets.UTF_8);
      } else {
        URL resourceUrl = this.getClass().getClassLoader().getResource(resourcePath);
        if (resourceUrl != null) {
          File file = new File(resourceUrl.getFile());
          result = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        }
      }
      return result;
    } catch (IOException ex) {
      throw SneakyThrows.propagate(ex);
    }
  }
}
