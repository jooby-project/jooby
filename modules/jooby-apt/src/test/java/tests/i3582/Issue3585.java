/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3582;

import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class Issue3585 {

  @Test
  public void shouldGenerateDefaultAnnotation() throws Exception {
    new ProcessorRunner(new C3582())
        .withSourceCode(
            source -> {
              assertTrue(source.contains(".setAttributes(java.util.Map.of("));
              assertTrue(source.contains("\"Annotation3582\", \"\"))"));
            });
  }
}
