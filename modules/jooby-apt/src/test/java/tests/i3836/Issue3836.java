/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3836;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class Issue3836 {
  @Test
  public void shouldSupportNameAttribute() throws Exception {
    new ProcessorRunner(new C3836())
        .withSourceCode(
            source -> {
              Assertions.assertTrue(
                  source.contains(
                      "return"
                          + " c.oddNameWithNameAttribute(ctx.query(\"some-http\").valueOrNull());"));
            });
  }
}
