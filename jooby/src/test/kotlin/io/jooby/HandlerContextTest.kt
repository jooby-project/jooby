package io.jooby

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class HandlerContextTest {
  @Test
  @DisplayName("Make sure we sync file name with RouteAnalyzer")
  fun classname() {
    assertEquals("io.jooby.HandlerContext", HandlerContext::class.java.name)
  }
}
