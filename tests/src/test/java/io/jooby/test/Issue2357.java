/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import org.junit.jupiter.api.Assertions;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

class Issue2357 {

  @ServerTest
  public void headersShouldBeCaseInsensitive(ServerTestRunner runner) {
    runner
        .define(
            app ->
                app.get(
                    "/",
                    ctx -> {
                      Assertions.assertEquals("value1", ctx.header().get("x-header1").value());
                      Assertions.assertEquals("value1", ctx.header().get("X-HEADER1").value());
                      Assertions.assertEquals("value1", ctx.header().get("X-hEaDeR1").value());
                      Assertions.assertEquals("value1", ctx.header("x-header1").value());
                      Assertions.assertEquals("value1", ctx.header("X-HEADER1").value());
                      Assertions.assertEquals("value1", ctx.header("X-hEaDeR1").value());
                      Assertions.assertEquals("value1", ctx.headerMap().get("x-header1"));
                      Assertions.assertEquals("value1", ctx.headerMap().get("X-HEADER1"));
                      Assertions.assertEquals("value1", ctx.headerMap().get("X-hEaDeR1"));
                      Assertions.assertEquals("value1", ctx.header().toMultimap().get("x-header1").get(0));
                      Assertions.assertEquals("value1", ctx.header().toMultimap().get("X-HEADER1").get(0));
                      Assertions.assertEquals("value1", ctx.header().toMultimap().get("X-hEaDeR1").get(0));
                      return "OK";
                    }))
        .ready(
            http ->
                http.header("x-header1", "value1")
                    .get(
                        "/",
                        rsp -> {
                          Assertions.assertEquals(200, rsp.code());
                        }));
  }
}
