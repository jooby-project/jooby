/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3761;

import io.jooby.apt.ProcessorRunner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class Issue3761 {
  @Test
  public void shouldGenerateDefaultValues() throws Exception {
    new ProcessorRunner(new C3761())
        .withSourceCode(Issue3761::assertSourceCodeRespectDefaultValues);
  }

  @Test
  public void shouldGenerateJakartaDefaultValues() throws Exception {
    new ProcessorRunner(new C3761Jakarta())
        .withSourceCode(Issue3761::assertSourceCodeRespectDefaultValues);
  }

  private static void assertSourceCodeRespectDefaultValues(String source) {
    assertTrue(source.contains("return c.number(ctx.query(\"num\", \"5\").intValue());"));
    assertTrue(source.contains("return c.unset(ctx.query(\"unset\").valueOrNull());"));
    assertTrue(
        source.contains("return c.emptySet(ctx.query(\"emptySet\", \"\").value());"));
    assertTrue(
        source.contains("return c.string(ctx.query(\"stringVal\", \"Hello\").value());"));
    assertTrue(
        source.contains("return c.bool(ctx.form(\"boolVal\", \"false\").booleanValue());"));
  }
}
