/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby

import io.jooby.Router.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

internal class RouterCoroutineScope(override val coroutineContext: CoroutineContext) : CoroutineScope

class CoroutineRouter(val coroutineStart: CoroutineStart, val router: Router) {

  val coroutineScope: CoroutineScope by lazy {
    RouterCoroutineScope(router.worker.asCoroutineDispatcher())
  }

  private var extendCoroutineContext: (CoroutineContext) -> CoroutineContext = { it }
  fun launchContext(block: (CoroutineContext) -> CoroutineContext) {
    extendCoroutineContext = block
  }

  @RouterDsl
  fun get(pattern: String, handler: suspend HandlerContext.() -> Any) =
    route(GET, pattern, handler)

  @RouterDsl
  fun post(pattern: String, handler: suspend HandlerContext.() -> Any) =
    route(POST, pattern, handler)

  @RouterDsl
  fun put(pattern: String, handler: suspend HandlerContext.() -> Any) =
    route(PUT, pattern, handler)

  @RouterDsl
  fun delete(pattern: String, handler: suspend HandlerContext.() -> Any) =
    route(DELETE, pattern, handler)

  @RouterDsl
  fun patch(pattern: String, handler: suspend HandlerContext.() -> Any) =
    route(PATCH, pattern, handler)

  @RouterDsl
  fun head(pattern: String, handler: suspend HandlerContext.() -> Any) =
    route(HEAD, pattern, handler)

  @RouterDsl
  fun trace(pattern: String, handler: suspend HandlerContext.() -> Any) =
    route(TRACE, pattern, handler)

  @RouterDsl
  fun options(pattern: String, handler: suspend HandlerContext.() -> Any) =
    route(OPTIONS, pattern, handler)

  fun route(method: String, pattern: String, handler: suspend HandlerContext.() -> Any): Route =
    router.route(method, pattern) { ctx ->
      launch(ctx) {
        val result = handler(HandlerContext(ctx))
        if (result != ctx) {
          ctx.render(result)
        }
      }
    }.setHandle(handler).attribute("coroutine", true)

  internal fun launch(ctx: Context, block: suspend CoroutineScope.() -> Unit) {
    val exceptionHandler = CoroutineExceptionHandler { _, x -> ctx.sendError(x) }
    coroutineScope.launch(extendCoroutineContext(exceptionHandler), coroutineStart, block)
  }
}
