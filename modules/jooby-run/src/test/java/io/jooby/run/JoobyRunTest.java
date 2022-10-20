/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.run;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.modules.ModuleClassLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class JoobyRunTest {
  @Test
  @DisplayName(
      "Make sure of module class loader name. It is required from Jooby when loading joobyRun hook")
  public void modularClassLoaderName() {
    assertEquals("org.jboss.modules.ModuleClassLoader", ModuleClassLoader.class.getName());
  }

  @Test
  @DisplayName(
      "ServerRef is loaded at runtime by Jooby. It let jooby:run to hook bootstrap process")
  public void serverRefName() {
    assertEquals(JoobyRun.SERVER_REF, ServerRef.class.getName());
  }
}
