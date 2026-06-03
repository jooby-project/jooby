/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

public class Issue3963 {

  @Test
  void logVariableCannotBeStatic() throws NoSuchFieldException {
    var field = Jooby.class.getDeclaredField("log");
    assertFalse(Modifier.isStatic(field.getModifiers()));
  }
}
