/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.kt

import io.jooby.*
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class KotlinContextClassesTest {

  private val ctx: Context = mockk()

  @Test
  fun testAfterContext() {
    val result = "success"
    val failure = null
    val context = AfterContext(ctx, result, failure)

    assertEquals(ctx, context.ctx)
    assertEquals(result, context.result)
    assertEquals(failure, context.failure)
  }

  @Test
  fun testFilterContext() {
    val next: Route.Handler = mockk()
    val context = FilterContext(ctx, next)

    assertEquals(ctx, context.ctx)
    assertEquals(next, context.next)
  }

  @Test
  fun testHandlerContext() {
    val context = HandlerContext(ctx)

    assertEquals(ctx, context.ctx)
    // Check serializability (as it implements Serializable)
    assertNotNull(context as java.io.Serializable)
  }

  @Test
  fun testErrorHandlerContext() {
    val cause = RuntimeException("error")
    val statusCode = StatusCode.BAD_REQUEST
    val context = ErrorHandlerContext(ctx, cause, statusCode)

    assertEquals(ctx, context.ctx)
    assertEquals(cause, context.cause)
    assertEquals(statusCode, context.statusCode)
    assertNotNull(context as java.io.Serializable)
  }

  @Test
  fun testWebSocketInitContext() {
    val configurer: WebSocketConfigurer = mockk()
    val context = WebSocketInitContext(ctx, configurer)

    assertEquals(ctx, context.ctx)
    assertEquals(configurer, context.configurer)
  }

  @Test
  fun testServerSentHandler() {
    val sse: ServerSentEmitter = mockk()
    val context = ServerSentHandler(ctx, sse)

    assertEquals(ctx, context.ctx)
    assertEquals(sse, context.sse)
  }
}
