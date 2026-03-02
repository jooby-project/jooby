/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3863;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class Issue3863 {
  @Test
  public void shouldGenerateTrpcHandler() throws Exception {
    new ProcessorRunner(new C3863())
        .withSourceCode(
            source -> {
              System.out.println(source);
            });
  }
}
