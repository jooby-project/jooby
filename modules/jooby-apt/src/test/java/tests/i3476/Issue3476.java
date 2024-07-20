/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3476;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class Issue3476 {

  @Test
  public void shouldGenerateGenerics() throws Exception {
    new ProcessorRunner(new C3476())
        .withSource(
            source -> {
              assertTrue(source.toString().contains("public <T> java.util.List<T> get("));
              assertTrue(source.toString().contains("return (java.util.List<T>) c.get("));
              assertTrue(
                  source
                      .toString()
                      .contains(
                          "c.box(ctx.query(\"box\").isMissing() ?"
                              + " ctx.query().toNullable(tests.i3476.Box.class) :"
                              + " ctx.query(\"box\").toNullable(tests.i3476.Box.class))"));
            });
  }
}
