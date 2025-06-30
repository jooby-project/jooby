/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3460;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class Issue3460 {

  @Test
  public void shouldNotUseJakartaProvider() throws Exception {
    new ProcessorRunner(new C3460())
        .withRouter(
            (app, source) -> {
              assertTrue(source.toString().contains("C3460_(io.jooby.SneakyThrows.Supplier<"));
            });
  }
}
