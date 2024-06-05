/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class CustomRouterNameTest {

  @Test
  public void generateCustomName() throws Exception {
    var prefix = "C";
    var suffix = "Router";
    var expectedClassName =
        CustomRouterName.class.getPackage().getName()
            + "."
            + prefix
            + CustomRouterName.class.getSimpleName()
            + suffix;
    new ProcessorRunner(
            new CustomRouterName(),
            Map.of("jooby.routerPrefix", prefix, "jooby.routerSuffix", suffix))
        .withRouter(
            (app, source) -> {
              assertEquals(expectedClassName, source.packageName + "." + source.typeSpec.name);
            });
  }
}
