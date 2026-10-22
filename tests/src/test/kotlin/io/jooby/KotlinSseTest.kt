/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby

import io.jooby.internal.mvc.KotlinSseController_
import io.jooby.value.Value
import io.jooby.value.ValueFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*

class KotlinSseTest {
  @Test
  fun shouldSupportKotlinSseControllers() {

    val app = Jooby().apply { mvc(KotlinSseController_()) }

    assertEquals(
      listOf("/kotlin-sse/events", "/kotlin-sse/alias", "/kotlin-sse/{name}"),
      app.routes.map { it.pattern })

    val ctx = mock(Context::class.java, RETURNS_SELF)
    val emitter = mock(ServerSentEmitter::class.java)
    `when`(emitter.context).thenReturn(ctx)
    `when`(ctx.path("name")).thenReturn(Value.value(ValueFactory(), "name", "Ada"))
    `when`(ctx.requestPath).thenReturn("/kotlin-sse/Ada")
    doAnswer { call ->
      call.getArgument<ServerSentEmitter.Handler>(0).handle(emitter)
      ctx
    }.`when`(ctx).upgrade(any(ServerSentEmitter.Handler::class.java))

    app.routes.forEach { route ->
      assertEquals("SSE", route.method)
      route.handler.apply(ctx)
    }

    verify(emitter, times(2)).send("Welcome")
    verify(emitter).send("Ada:/kotlin-sse/Ada")
    verify(emitter, times(3)).close()
    verify(ctx, times(3)).setResponseType("text/event-stream; charset=utf-8")
  }
}
