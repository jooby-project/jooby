/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class KtGeneratorTest {

  @Test
  public void generateCustomName() throws Exception {
    var prefix = "";
    var suffix = "_";
    var expectedClassName =
        CustomRouterName.class.getPackage().getName()
            + "/"
            + prefix
            + CustomRouterName.class.getSimpleName()
            + suffix
            + ".java";
    new ProcessorRunner(new CustomRouterName())
        .withRouter(
            (app, source) -> {
              // assertEquals(expectedClassName, source.getName());
            });
  }
}
