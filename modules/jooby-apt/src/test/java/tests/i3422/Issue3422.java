/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3422;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class Issue3422 {

  @Test
  public void generateCustomHandlerFunction() throws Exception {
    new ProcessorRunner(
            new C3422(),
            Map.of("jooby.handler", ReactiveTypeGenerator.class.getName(), "jooby.debug", false))
        .withRouter(
            (app, source) -> {
              assertTrue(source.toString().contains(", toReactive(this::reactiveType)"));
            });
  }
}
