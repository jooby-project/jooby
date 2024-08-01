/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3488;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.jooby.internal.apt.CodeBlock;

public class Issue3488 {

  @Test
  public void shouldGenerateKtGenerics() {
    assertEquals(
        "pgk.Results<List<pgk.UserInfo>>",
        CodeBlock.type(true, "pgk.Results<java.util.List<pgk.UserInfo>>"));
  }
}
