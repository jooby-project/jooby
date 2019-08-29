/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mvc

import io.jooby.Context
import io.jooby.CoroutineRouter
import io.jooby.Route
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

class CoroutineLauncher(val next: Route.Handler) : Route.Handler {
  override fun apply(ctx: Context): Any {
    val router = ctx.router.attribute<CoroutineRouter>("coroutineRouter")
    val exceptionHandler = CoroutineExceptionHandler { _, x ->
      ctx.sendError(x)
    }
    router.coroutineScope.launch(exceptionHandler, router.coroutineStart) {
      val result = suspendCoroutineUninterceptedOrReturn<Any> {
        ctx.attribute("___continuation", it)
        next.apply(ctx)
      }
      if (!ctx.isResponseStarted) {
        ctx.render(result!!)
      }
    }
    return ctx
  }
}
