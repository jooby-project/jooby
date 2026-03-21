/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3830;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class Issue3830 {
  @Test
  public void shouldGenerateMcpServer() throws Exception {
    new ProcessorRunner(new ExampleServer())
        .withSourceCode(
            source -> {
              System.out.println(source);
            });
  }
}
