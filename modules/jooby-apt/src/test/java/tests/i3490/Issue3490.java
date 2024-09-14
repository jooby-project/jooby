/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3490;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class Issue3490 {

  @Test
  public void shouldNotGeneratePrimitiveOnKotlinGenerics() throws IOException {
    new ProcessorRunner(new C3490())
        .withSourceCode(
            true,
            source -> {
              assertTrue(
                  source.contains(
                      ".setReturnType(io.jooby.Reified.getParameterized(tests.i3490.Box3490::class.java,"
                          + " Integer::class.java).getType())"));
            });
  }
}
