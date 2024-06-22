/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3455;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class Issue3455 {

  @Test
  public void shouldEscapeJavaSpecialCharacters() throws Exception {
    new ProcessorRunner(new C3455())
        .withRouter(
            (app, source) -> {
              assertTrue(source.toString().contains("\"/\\\"path/required\\\"-string-param\""));
              assertTrue(source.toString().contains("\"test\\\"ttttt\""));
              assertTrue(source.toString().contains("\"value\\\"\""));
            });
  }
}
