/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3853;

import static io.jooby.i3853.U3853.createUser;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.*;

import io.jooby.Extension;
import io.jooby.Projected;
import io.jooby.Projection;
import io.jooby.avaje.jsonb.AvajeJsonbModule;
import io.jooby.jackson.JacksonModule;
import io.jooby.jackson3.Jackson3Module;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue3853 {

  Projection<U3853> STUB = Projection.of(U3853.class).include("(id, name)");

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

              app.get(
                  "/stub",
                  ctx -> {
                    return Projected.wrap(createUser(), STUB);
                  });

              app.get(
                  "/stub-list",
                  ctx -> {
                    return Projected.wrap(List.of(createUser())).include("(id, name)");
                  });

              app.get(
                  "/stub-empty-list",
                  ctx -> {
                    return Projected.wrap(List.of()).include("(id, name)");
                  });

              app.get(
                  "/stub-set",
                  ctx -> {
                    return Projected.wrap(Set.of(createUser())).include("(id, name)");
                  });
              app.get(
                  "/stub-optional",
                  ctx -> {
                    return Projected.wrap(Optional.of(createUser())).include("(id, name)");
                  });
              app.get(
                  "/stub-optional-null",
                  ctx -> {
                    return Projected.wrap(Optional.empty()).include("(id, name)");
                  });
              app.get(
                  "/stub/meta",
                  ctx -> {
                    return Projected.wrap(createUser()).include("(id, meta(target))");
                  });
              app.get(
                  "/stub/roles",
                  ctx -> {
                    return Projected.wrap(createUser()).include("(id, roles(name))");
                  });
              app.get(
                  "/stub/address",
                  ctx -> {
                    return Projected.wrap(createUser()).include("(id, name, address(*))");
                  });
              app.get(
                  "/stub/address-stub",
                  ctx -> {
                    return Projected.wrap(createUser()).include("(id, name, address(city))");
                  });
              app.get(
                  "/stub/address-loc-lat",
                  ctx -> {
                    return Projected.wrap(createUser()).include("(id, name, address(loc(lat)))");
                  });
            })
        .ready(
            http -> {
              http.get(
                  "/stub/meta",
                  rsp -> {
                    assertThat(rsp.body().string())
                        .isEqualToIgnoringNewLines(
                            """
                            {"id":"cobb-001","meta":{"target":"Robert Fischer","objective":"Inception","status":"Synchronizing Kicks"}}
                            """);
                  });
              http.get(
                  "/stub",
                  rsp -> {
                    assertThat(rsp.body().string())
                        .isEqualToIgnoringNewLines(
                            """
                            {"id":"cobb-001","name":"Dom Cobb"}
                            """);
                  });
              http.get(
                  "/stub-list",
                  rsp -> {
                    assertThat(rsp.body().string())
                        .isEqualToIgnoringNewLines(
                            """
                            [{"id":"cobb-001","name":"Dom Cobb"}]
                            """);
                  });
              http.get(
                  "/stub-set",
                  rsp -> {
                    assertThat(rsp.body().string())
                        .isEqualToIgnoringNewLines(
                            """
                            [{"id":"cobb-001","name":"Dom Cobb"}]
                            """);
                  });
              http.get(
                  "/stub-optional",
                  rsp -> {
                    assertThat(rsp.body().string())
                        .isEqualToIgnoringNewLines(
                            """
                            {"id":"cobb-001","name":"Dom Cobb"}
                            """);
                  });
              http.get(
                  "/stub-empty-list",
                  rsp -> {
                    assertThat(rsp.body().string())
                        .isEqualToIgnoringNewLines(
                            """
                            []
                            """);
                  });
              http.get(
                  "/stub-optional-null",
                  rsp -> {
                    assertThat(rsp.body().string()).isEqualTo("null");
                  });
              http.get(
                  "/stub/address",
                  rsp -> {
                    assertThat(rsp.body().string())
                        .isEqualToIgnoringNewLines(
                            """
                            {"id":"cobb-001","name":"Dom Cobb","address":{"city":"Snow Fortress (Level 3)","loc":{"lat":80.0,"lon":-20.0}}}
                            """);
                  });
              http.get(
                  "/stub/address-stub",
                  rsp -> {
                    assertThat(rsp.body().string())
                        .isEqualToIgnoringNewLines(
                            """
                            {"id":"cobb-001","name":"Dom Cobb","address":{"city":"Snow Fortress (Level 3)"}}
                            """);
                  });
              http.get(
                  "/stub/address-loc-lat",
                  rsp -> {
                    assertThat(rsp.body().string())
                        .isEqualToIgnoringNewLines(
                            """
                            {"id":"cobb-001","name":"Dom Cobb","address":{"loc":{"lat":80.0}}}
                            """);
                  });
              http.get(
                  "/stub/roles",
                  rsp -> {
                    assertThat(rsp.body().string())
                        .isEqualToIgnoringNewLines(
                            """
                            {"id":"cobb-001","roles":[{"name":"The Extractor"},{"name":"The Architect"},{"name":"The Point Man"},{"name":"The Forger"}]}
                            """);
                  });
            });
  }

  @ServerTest
  public void jackson2ShouldNotThrowInvalidDefinitionException(ServerTestRunner runner) {
    jacksonShouldNotThrowInvalidDefinitionException(runner, new JacksonModule());
  }

  @ServerTest
  public void jackson3ShouldNotThrowInvalidDefinitionException(ServerTestRunner runner) {
    jacksonShouldNotThrowInvalidDefinitionException(runner, new Jackson3Module());
  }

  public void jacksonShouldNotThrowInvalidDefinitionException(
      ServerTestRunner runner, Extension extension) {
    runner
        .define(
            app -> {
              app.install(extension);
              app.get(
                  "/user",
                  ctx -> {
                    return createUser();
                  });
            })
        .ready(
            http -> {
              http.get(
                  "/user",
                  rsp -> {
                    assertThat(rsp.body().string())
                        .isEqualToIgnoringNewLines(
                            """
                            {"id":"cobb-001","name":"Dom Cobb","address":{"city":"Snow Fortress (Level 3)","loc":{"lat":80.0,"lon":-20.0}},"roles":[{"name":"The Extractor","level":10},{"name":"The Architect","level":9},{"name":"The Point Man","level":8},{"name":"The Forger","level":8}],"meta":{"target":"Robert Fischer","objective":"Inception","status":"Synchronizing Kicks"}}
                            """);
                  });
            });
  }
}
