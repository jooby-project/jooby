/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal class RouterCoroutineScope(coroutineContext: CoroutineContext) : CoroutineScope {
  override val coroutineContext = coroutineContext
}

class CoroutineRouter(val coroutineStart: CoroutineStart, val router: Router) {

  val coroutineScope: CoroutineScope by lazy {
    RouterCoroutineScope(router.worker.asCoroutineDispatcher())
  }

  @RouterDsl
  fun get(pattern: String, handler: suspend HandlerContext.() -> Any): Route {
    return route(Router.GET, pattern, handler)
  }

  @RouterDsl
  fun post(pattern: String, handler: suspend HandlerContext.() -> Any): Route {
    return route(Router.POST, pattern, handler)
  }

  @RouterDsl
  fun put(pattern: String, handler: suspend HandlerContext.() -> Any): Route {
    return route(Router.PUT, pattern, handler)
  }

  @RouterDsl
  fun delete(pattern: String, handler: suspend HandlerContext.() -> Any): Route {
    return route(Router.DELETE, pattern, handler)
  }

  @RouterDsl
  fun patch(pattern: String, handler: suspend HandlerContext.() -> Any): Route {
    return route(Router.PATCH, pattern, handler)
  }

  @RouterDsl
  fun head(pattern: String, handler: suspend HandlerContext.() -> Any): Route {
    return route(Router.HEAD, pattern, handler)
  }

  @RouterDsl
  fun trace(pattern: String, handler: suspend HandlerContext.() -> Any): Route {
    return route(Router.TRACE, pattern, handler)
  }

  @RouterDsl
  fun options(pattern: String, handler: suspend HandlerContext.() -> Any): Route {
    return route(Router.OPTIONS, pattern, handler)
  }

  fun route(method: String, pattern: String, handler: suspend HandlerContext.() -> Any): Route {
    return router.route(method, pattern) { ctx ->
      val xhandler = CoroutineExceptionHandler { _, x ->
        ctx.sendError(x)
      }
      coroutineScope.launch(xhandler, coroutineStart) {
        val result = handler(HandlerContext(ctx))
        if (result != ctx) {
          ctx.render(result)
        }
      }
    }.setHandle(handler)
     .attribute("coroutine", true)
  }
}
