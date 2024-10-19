/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3562;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import io.jooby.ModelAndView;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.pebble.PebbleModule;
import io.jooby.thymeleaf.ThymeleafModule;

public class Issue3562 {
  @ServerTest
  public void unsupportedModelAndView(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.install(new ThymeleafModule());
              app.install(new PebbleModule());

              app.get(
                  "/3562/thymeleaf",
                  ctx -> new ModelAndView<>("index.html", Map.of("name", "thymeleaf")));
              app.get(
                  "/3562/pebble",
                  ctx -> new ModelAndView<>("index.pebble", Map.of("name", "Pebble")));

              app.error((ctx, cause, status) -> ctx.send(cause.getMessage()));
            })
        .ready(
            client -> {
              client.get(
                  "/3562/thymeleaf",
                  rsp -> {
                    assertEquals(
                        "Only io.jooby.MapModelAndView are supported", rsp.body().string().trim());
                  });
              client.get(
                  "/3562/pebble",
                  rsp -> {
                    assertEquals(
                        "Only io.jooby.MapModelAndView are supported", rsp.body().string().trim());
                  });
            });
  }
}
