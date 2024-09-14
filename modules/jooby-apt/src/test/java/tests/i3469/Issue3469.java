/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3469;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class Issue3469 {

  @Test
  public void shouldGenerateConstructors() throws Exception {
    new ProcessorRunner(new C3469(new Foo3469(), List.of(new Bar3469())))
        .withJavaObject(
            source -> {
              assertTrue(
                  source
                      .toString()
                      .contains(
                          "public C3469_(tests.i3469.Foo3469 foo,"
                              + " java.util.List<tests.i3469.Bar3469> bar)"));
              assertTrue(source.toString().contains("public C3469_(tests.i3469.Foo3469 foo)"));
              assertFalse(source.toString().contains("public C3469_()"));
            });
  }

  @Test
  public void shouldGenerateFieldInjectWithDefaultConstructor() throws Exception {
    new ProcessorRunner(new C3469FieldInject())
        .withJavaObject(
            source -> {
              assertTrue(source.toString().contains("this(C3469FieldInject.class);"));
            });
  }
}
