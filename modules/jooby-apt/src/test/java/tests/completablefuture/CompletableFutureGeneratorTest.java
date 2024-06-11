/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.completablefuture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class CompletableFutureGeneratorTest {

  @Test
  public void generateCompletableFuture() throws Exception {
    new ProcessorRunner(new CCompletableFuture())
        .withRouter(
            (app, source) -> {
              assertTrue(
                  source.getCharContent(false).toString().contains(", concurrent(this::future)"));
            });
  }
}
