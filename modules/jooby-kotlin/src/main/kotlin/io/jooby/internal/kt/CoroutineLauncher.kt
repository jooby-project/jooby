/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.kt

import io.jooby.Context
import io.jooby.Route
import io.jooby.kt.CoroutineRouter
import io.jooby.kt.HandlerContext
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

/** Used by compiled MVC-style routes with suspend functions */
class CoroutineLauncher(val next: Route.Handler) : Route.Handler {
  override fun apply(ctx: Context) =
    ctx.also {
      val router = ctx.router.attribute<CoroutineRouter>("coroutineRouter")
      router.launch(HandlerContext(ctx)) {
        val result =
          suspendCoroutineUninterceptedOrReturn<Any> {
            ctx.setAttribute("___continuation", it)
            next.apply(ctx)
          }
        ctx.route.after?.apply(ctx, result, null)
        if (!ctx.isResponseStarted) {
          ctx.render(result)
        }
      }
    }
}
