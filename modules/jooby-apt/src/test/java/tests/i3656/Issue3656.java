/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3656;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class Issue3656 {
  @Test
  public void shouldNotThrowErrorOnEmptyController() throws Exception {
    new ProcessorRunner(new C3656()).withSourceCode(Assertions::assertNull);
  }
}
