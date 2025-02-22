/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.kt

import io.jooby.*
import io.jooby.Router.*
import java.util.function.Predicate
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KClass
import kotlinx.coroutines.*

internal class RouterCoroutineScope(override val coroutineContext: CoroutineContext) :
  CoroutineScope

class CoroutineRouter(val coroutineStart: CoroutineStart, val router: Router) {

  val coroutineScope: CoroutineScope by lazy {
    RouterCoroutineScope(router.worker.asCoroutineDispatcher())
  }

  private var errorHandler: suspend ErrorHandlerContext.() -> Unit = FALLBACK_ERROR_HANDLER

  private var extraCoroutineContextProvider: HandlerContext.() -> CoroutineContext = {
    EmptyCoroutineContext
  }

  fun launchContext(provider: HandlerContext.() -> CoroutineContext) {
    extraCoroutineContextProvider = provider
  }

  /**
   * Add a custom error handler that matches the given status code.
   *
   * @param statusCode Status code.
   * @param handler Error handler.
   * @return This router.
   */
  @RouterDsl
  fun error(
    statusCode: StatusCode,
    handler: suspend ErrorHandlerContext.() -> Unit,
  ): CoroutineRouter {
    return error({ it: StatusCode -> statusCode == it }, handler)
  }

  /**
   * Add a custom error handler that matches the given exception type.
   *
   * @param type Exception type.
   * @param handler Error handler.
   * @return This router.
   */
  @RouterDsl
  fun error(
    type: KClass<Throwable>,
    handler: suspend ErrorHandlerContext.() -> Unit,
  ): CoroutineRouter {
    return error {
      if (type.java.isInstance(cause) || type.java.isInstance(cause.cause)) {
        handler.invoke(this)
      }
    }
  }

  /**
   * Add a custom error handler that matches the given predicate.
   *
   * @param predicate Status code filter.
   * @param handler Error handler.
   * @return This router.
   */
  @RouterDsl
  fun error(
    predicate: Predicate<StatusCode>,
    handler: suspend ErrorHandlerContext.() -> Unit,
  ): CoroutineRouter {
    return error {
      if (predicate.test(statusCode)) {
        handler.invoke(this)
      }
    }
  }

  /**
   * Add a custom error handler.
   *
   * @param handler Error handler.
   * @return This router.
   */
  @RouterDsl
  fun error(handler: suspend ErrorHandlerContext.() -> Unit): CoroutineRouter {
    val chain =
      fun(
        current: suspend ErrorHandlerContext.() -> Unit,
        next: suspend ErrorHandlerContext.() -> Unit,
      ): suspend ErrorHandlerContext.() -> Unit {
        return {
          current(this)
          if (!ctx.isResponseStarted) {
            next(this)
          }
        }
      }
    errorHandler =
      if (errorHandler == FALLBACK_ERROR_HANDLER) handler else chain(errorHandler, handler)
    return this
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
          try {
            val result = handler(handlerContext)
            ctx.route.after?.apply(ctx, result, null)
            if (result != ctx && !ctx.isResponseStarted) {
              ctx.render(result)
            }
          } catch (cause: Throwable) {
            try {
              ctx.route.after?.apply(ctx, null, cause)
            } finally {
              errorHandler.invoke(ErrorHandlerContext(ctx, cause, router.errorCode(cause)))
            }
          }
        }
        // Return context to mark as handled
        ctx
      }
      .setHandle(handler)

  internal fun launch(handlerContext: HandlerContext, block: suspend CoroutineScope.() -> Unit) {
    // Global catch-all exception handler
    val exceptionHandler = CoroutineExceptionHandler { _, x ->
      val ctx = handlerContext.ctx
      ctx.route.after?.apply(ctx, null, x)
      ctx.sendError(x)
    }
    val requestScope = RequestScope.threadLocal().asContextElement()
    val coroutineContext =
      exceptionHandler + requestScope + handlerContext.extraCoroutineContextProvider()
    coroutineScope.launch(coroutineContext, coroutineStart, block)
  }

  private companion object {
    private val FALLBACK_ERROR_HANDLER: suspend ErrorHandlerContext.() -> Unit = {
      ctx.sendError(cause, statusCode)
    }
  }
}
