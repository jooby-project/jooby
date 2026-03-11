/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3853;

import static org.assertj.core.api.Assertions.assertThat;

import io.jooby.Extension;
import io.jooby.avaje.jsonb.AvajeJsonbModule;
import io.jooby.jackson.JacksonModule;
import io.jooby.jackson3.Jackson3Module;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue3853Mvc {

  @ServerTest
  public void shouldProjectJackson2Data(ServerTestRunner runner) {
    shouldProjectData(runner, new JacksonModule());
  }

  @ServerTest
  public void shouldProjectJackson3Data(ServerTestRunner runner) {
    shouldProjectData(runner, new Jackson3Module());
  }

  @ServerTest
  public void shouldProjectAvajeData(ServerTestRunner runner) {
    shouldProjectData(runner, new AvajeJsonbModule());
  }

  public void shouldProjectData(ServerTestRunner runner, Extension extension) {
    runner
        .define(
            app -> {
              app.install(extension);

              app.mvc(new C3853_());
            })
        .ready(
            http -> {
              http.get(
                  "/3853/stub",
                  rsp -> {
                    assertThat(rsp.body().string())
                        .isEqualToIgnoringNewLines(
                            """
                            {"id":"cobb-001","name":"Dom Cobb"}
                            """);
                  });
              http.get(
                  "/3853/optional",
                  rsp -> {
                    assertThat(rsp.body().string())
                        .isEqualToIgnoringNewLines(
                            """
                            {"id":"cobb-001","name":"Dom Cobb"}
                            """);
                  });
              http.get(
                  "/3853/list",
                  rsp -> {
                    assertThat(rsp.body().string())
                        .isEqualToIgnoringNewLines(
                            """
                            [{"id":"cobb-001"}]
                            """);
                  });
              http.get(
                  "/3853/projected",
                  rsp -> {
                    assertThat(rsp.body().string())
                        .isEqualToIgnoringNewLines(
                            """
                            {"id":"cobb-001","name":"Dom Cobb"}
                            """);
                  });
              http.get(
                  "/3853/projectedProjection",
                  rsp -> {
                    assertThat(rsp.body().string())
                        .isEqualToIgnoringNewLines(
                            """
                            {"id":"cobb-001","name":"Dom Cobb"}
                            """);
                  });
            });
  }
}
