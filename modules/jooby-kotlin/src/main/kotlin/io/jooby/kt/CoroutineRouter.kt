/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.kt

import io.jooby.Route
import io.jooby.Router
import io.jooby.Router.DELETE
import io.jooby.Router.GET
import io.jooby.Router.HEAD
import io.jooby.Router.OPTIONS
import io.jooby.Router.PATCH
import io.jooby.Router.POST
import io.jooby.Router.PUT
import io.jooby.Router.TRACE
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

internal class RouterCoroutineScope(override val coroutineContext: CoroutineContext) :
  CoroutineScope

class CoroutineRouter(val coroutineStart: CoroutineStart, val router: Router) {

  val coroutineScope: CoroutineScope by lazy {
    RouterCoroutineScope(router.worker.asCoroutineDispatcher())
  }

  private var extraCoroutineContextProvider: HandlerContext.() -> CoroutineContext = {
    EmptyCoroutineContext
  }
  fun launchContext(provider: HandlerContext.() -> CoroutineContext) {
    extraCoroutineContextProvider = provider
  }

  @RouterDsl
  fun get(pattern: String, handler: suspend HandlerContext.() -> Any) = route(GET, pattern, handler)

  @RouterDsl
  fun post(pattern: String, handler: suspend HandlerContext.() -> Any) =
    route(POST, pattern, handler)

  @RouterDsl
  fun put(pattern: String, handler: suspend HandlerContext.() -> Any) = route(PUT, pattern, handler)

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
    router
      .route(method, pattern) { ctx ->
        val handlerContext = HandlerContext(ctx)
        launch(handlerContext) {
          val result = handler(handlerContext)
          ctx.route.after?.apply(ctx, result, null)
          if (result != ctx && !ctx.isResponseStarted) {
            ctx.render(result)
          }
          // Return context to mark as handled
          ctx
        }
        // Return context to mark as handled
        ctx
      }
      .setHandle(handler)

  internal fun launch(handlerContext: HandlerContext, block: suspend CoroutineScope.() -> Unit) {
    val exceptionHandler = CoroutineExceptionHandler { _, x ->
      val ctx = handlerContext.ctx
      ctx.route.after?.apply(ctx, null, x)
      ctx.sendError(x)
    }
    val coroutineContext = exceptionHandler + handlerContext.extraCoroutineContextProvider()
    coroutineScope.launch(coroutineContext, coroutineStart, block)
  }
}
