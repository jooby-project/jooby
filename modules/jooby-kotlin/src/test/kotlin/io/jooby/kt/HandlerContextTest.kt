/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.kt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class HandlerContextTest {
  @Test
  @DisplayName("Make sure we sync file name with RouteAnalyzer")
  fun classname() {
    assertEquals("io.jooby.kt.HandlerContext", HandlerContext::class.java.name)
  }
}
