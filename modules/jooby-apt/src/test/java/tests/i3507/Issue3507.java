/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3507;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class Issue3507 {

  @Test
  public void shouldGenerateNullSafeKtReturnType() throws IOException {
    new ProcessorRunner(new C3507())
        .withSource(
            true,
            source -> {
              assertTrue(source.contains("return c.get(ctx.query(\"query\").value())!!"));
            });
  }
}
