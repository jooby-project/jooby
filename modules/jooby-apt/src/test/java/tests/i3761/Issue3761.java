/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3761;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class Issue3761 {
  @Test
  public void shouldGenerateDefaultValues() throws Exception {
    new ProcessorRunner(new C3761())
        .withSourceCode(
            (source) -> {
              assertTrue(source.contains("return c.number(ctx.query(\"num\", \"5\").intValue());"));
              assertTrue(source.contains("return c.unset(ctx.query(\"unset\").valueOrNull());"));
              assertTrue(
                  source.contains("return c.emptySet(ctx.query(\"emptySet\", \"\").value());"));
              assertTrue(
                  source.contains("return c.string(ctx.query(\"stringVal\", \"Hello\").value());"));
            });
  }
}
