/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mvc

import io.jooby.Context
import io.jooby.CoroutineRouter
import io.jooby.HandlerContext
import io.jooby.Route
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

/**
 * Used by compiled MVC-style routes with suspend functions
 */
class CoroutineLauncher(val next: Route.Handler) : Route.Handler {
  override fun apply(ctx: Context) = ctx.also {
    val router = ctx.router.attribute<CoroutineRouter>("coroutineRouter")
    router.launch(HandlerContext(ctx)) {
      val result = suspendCoroutineUninterceptedOrReturn<Any> {
        ctx.attribute("___continuation", it)
        next.apply(ctx)
      }
      if (!ctx.isResponseStarted) {
        ctx.render(result)
      }
    }
  }
}
