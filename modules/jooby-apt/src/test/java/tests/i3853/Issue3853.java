/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3853;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class Issue3853 {
  @Test
  public void shouldSupportNameAttribute() throws Exception {
    new ProcessorRunner(new C3853())
        .withSourceCode(
            source -> {
              Assertions.assertTrue(
                  source.contains(
                      "return io.jooby.Projected.wrap(c.projectUser()).include(\"(id, name)\");"));
              Assertions.assertTrue(
                  source.contains(
                      "return io.jooby.Projected.wrap(c.findUser()).include(\"(id, name)\");"));
              Assertions.assertTrue(
                  source.contains(
                      "return io.jooby.Projected.wrap(c.findUsers()).include(\"(id, name)\");"));
              Assertions.assertTrue(source.contains("return c.projected();"));
            })
        .withSourceCode(
            true,
            source -> {
              Assertions.assertTrue(
                  source.contains(
                      "return io.jooby.Projected.wrap(c.projectUser()).include(\"(id, name)\")"));
              Assertions.assertTrue(
                  source.contains(
                      "return io.jooby.Projected.wrap(c.findUser()).include(\"(id, name)\")"));
              Assertions.assertTrue(
                  source.contains(
                      "return io.jooby.Projected.wrap(c.findUsers()).include(\"(id, name)\")"));
              Assertions.assertTrue(source.contains("return c.projected()"));
            });
  }
}
