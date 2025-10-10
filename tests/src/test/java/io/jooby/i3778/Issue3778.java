/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3778;

import static io.jooby.test.TestUtil.File_19kb;
import static io.jooby.test.TestUtil._19kb;
import static io.jooby.vertx.VertxHandler.vertx;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jooby.ExecutionMode;
import io.jooby.jackson.JacksonModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.vertx.VertxModule;
import io.jooby.vertx.VertxServer;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;

public class Issue3778 {

  @ServerTest(executionMode = {ExecutionMode.EVENT_LOOP, ExecutionMode.WORKER})
  public void shouldHandleVertxReactiveResponse(ServerTestRunner runner) {
    shouldHandleVertxReactiveResponse(runner, false);
  }

  @ServerTest(
      server = VertxServer.class,
      executionMode = {ExecutionMode.EVENT_LOOP, ExecutionMode.WORKER})
  public void shouldHandleVertxReactiveResponseOnVertx(ServerTestRunner runner) {
    shouldHandleVertxReactiveResponse(runner, true);
  }

  private void shouldHandleVertxReactiveResponse(ServerTestRunner runner, boolean onVertx) {
    runner
        .define(
            app -> {
              if (!onVertx) {
                app.install(new VertxModule(new VertxOptions().setEventLoopPoolSize(4)));
              }
              app.install(new JacksonModule());

              app.use(vertx());

              app.get(
                  "/3778/side-effect",
                  ctx -> {
                    var vertx = ctx.require(Vertx.class);
                    vertx.setTimer(
                        100,
                        t -> {
                          ctx.render(Map.of("timer", t));
                        });
                    return ctx;
                  });
              app.get(
                  "/3778/promise",
                  ctx -> {
                    var vertx = ctx.require(Vertx.class);
                    var promise = Promise.<List<String>>promise();
                    var result = new ArrayList<String>();
                    vertx.setTimer(
                        50,
                        t1 -> {
                          result.add("1");
                          vertx.setTimer(
                              50,
                              t2 -> {
                                result.add("2");
                                vertx.setTimer(
                                    50,
                                    t3 -> {
                                      result.add("3");
                                      promise.complete(result);
                                    });
                              });
                        });
                    return promise;
                  });
              app.get(
                  "/3778/future",
                  ctx -> {
                    var fs = ctx.require(FileSystem.class);
                    return fs.props(File_19kb.toString());
                  });
              app.get(
                  "/3778/asyncFile",
                  ctx -> {
                    var fs = ctx.require(FileSystem.class);
                    return fs.open(File_19kb.toString(), new OpenOptions());
                  });
            })
        .ready(
            http -> {
              var json = new ObjectMapper();
              http.get(
                  "/3778/side-effect",
                  rsp -> {
                    assertEquals("{\"timer\":0}", rsp.body().string());
                  });
              http.get(
                  "/3778/promise",
                  rsp -> {
                    assertEquals("[\"1\",\"2\",\"3\"]", rsp.body().string());
                  });
              http.get(
                  "/3778/future",
                  rsp -> {
                    var expected =
                        Map.of(
                            "directory",
                            false,
                            "symbolicLink",
                            false,
                            "regularFile",
                            true,
                            "other",
                            false);
                    assertEquals(expected, json.readValue(rsp.body().string(), Map.class));
                  });
              http.get(
                  "/3778/asyncFile",
                  rsp -> {
                    assertEquals(_19kb, rsp.body().string());
                  });
            });
  }
}
