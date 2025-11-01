/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3804;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class Issue3804 {
  @Test
  public void shouldDetectDIOnFieldsOfBaseClass() throws Exception {
    new ProcessorRunner(new C3804())
        .withSourceCode(
            source -> {
              assertTrue(source.contains("setup(ctx -> ctx.require(type));"));
            });
  }

  @Test
  public void shouldDetectDIOnFields() throws Exception {
    new ProcessorRunner(new C3804b())
        .withSourceCode(
            source -> {
              assertTrue(source.contains("setup(ctx -> ctx.require(type));"));
            });
  }

  @Test
  public void shouldDetectDIOnSetter() throws Exception {
    new ProcessorRunner(new C3804c())
        .withSourceCode(
            source -> {
              assertTrue(source.contains("setup(ctx -> ctx.require(type));"));
            });
  }
}
