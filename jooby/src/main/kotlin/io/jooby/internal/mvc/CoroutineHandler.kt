package io.jooby.internal.mvc

import io.jooby.Context
import io.jooby.Kooby
import io.jooby.Route
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.launch
import java.lang.reflect.Method
import javax.inject.Provider
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.kotlinFunction

class CoroutineHandler(val provider: Provider<Any>, val handler: Method, val argumentResolver: MvcHandler) : Route.Handler {
  override fun apply(ctx: Context): Any {
    val router = ctx.router() as Kooby
    val xhandler = CoroutineExceptionHandler { _, x ->
      ctx.sendError(x)
    }
    router.coroutineScope.launch(CoroutineName(handler.name) + xhandler, router.coroutineStart) {
      val instance = provider.get()
      val result = handler.kotlinFunction!!.callSuspend(instance, *argumentResolver.args(ctx))
      if (result != ctx) {
        ctx.render(result!!)
      }
    }
    return ctx
  }
}
