/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3941;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import io.jooby.ModelAndView;
import io.jooby.SessionStore;
import io.jooby.handlebars.HandlebarsModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.test.TestUtil;

public class Issue3941 {

  @ServerTest
  public void shouldTriggerNoSessionError(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.setSessionStore(SessionStore.UNSUPPORTED);
              app.install(
                  new HandlebarsModule(TestUtil.userdir("src", "test", "resources", "views")));
              app.get("/3814", ctx -> ModelAndView.map("index.hbs", Map.of("name", "3941")));
            })
        .ready(
            http -> {
              http.get(
                  "/3814",
                  rsp -> {
                    assertEquals("Hello 3941!", rsp.body().string().trim());
                  });
            });
  }
}
