/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3854;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class Issue3854 {
  @Test
  public void shouldSupportNameAttribute() throws Exception {
    new ProcessorRunner(new C3854())
        .withSourceCode(
            source -> {
              System.out.println(source);
            });
  }
}
