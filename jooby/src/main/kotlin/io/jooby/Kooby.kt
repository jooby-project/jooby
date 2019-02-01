package io.jooby

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPEALIAS, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class RouterDsl

internal class WorkerCoroutineScope(coroutineContext: CoroutineContext) : CoroutineScope {
  override val coroutineContext = coroutineContext
}

class ContextRef(val ctx: Context)

open class Kooby constructor() : Jooby() {

  var coroutineStart: CoroutineStart = CoroutineStart.DEFAULT

  private val coroutineScope: CoroutineScope by lazy {
    WorkerCoroutineScope(worker().asCoroutineDispatcher())
  }

  constructor(init: Kooby.() -> Unit) : this() {
    this.init()
  }

  @RouterDsl
  fun get(pattern: String = "/", handler: suspend ContextRef.() -> Any): Route {
    return route(Router.GET, pattern, handler)
  }

  @RouterDsl
  fun post(pattern: String = "/", handler: suspend ContextRef.() -> Any): Route {
    return route(Router.POST, pattern, handler)
  }

  @RouterDsl
  fun put(pattern: String = "/", handler: suspend ContextRef.() -> Any): Route {
    return route(Router.PUT, pattern, handler)
  }

  @RouterDsl
  fun delete(pattern: String = "/", handler: suspend ContextRef.() -> Any): Route {
    return route(Router.DELETE, pattern, handler)
  }

  @RouterDsl
  fun patch(pattern: String = "/", handler: suspend ContextRef.() -> Any): Route {
    return route(Router.PATCH, pattern, handler)
  }

  @RouterDsl
  fun head(pattern: String = "/", handler: suspend ContextRef.() -> Any): Route {
    return route(Router.HEAD, pattern, handler)
  }

  @RouterDsl
  fun trace(pattern: String = "/", handler: suspend ContextRef.() -> Any): Route {
    return route(Router.TRACE, pattern, handler)
  }

  @RouterDsl
  fun options(pattern: String = "/", handler: suspend ContextRef.() -> Any): Route {
    return route(Router.OPTIONS, pattern, handler)
  }

  @RouterDsl
  fun connect(pattern: String = "/", handler: suspend ContextRef.() -> Any): Route {
    return route(Router.CONNECT, pattern, handler)
  }

  @RouterDsl
  fun route(method: String, pattern: String, handler: suspend ContextRef.() -> Any): Route {
    return route(method, pattern) { ctx ->
      val xhandler = CoroutineExceptionHandler { _, x ->
        ctx.sendError(x)
      }
      coroutineScope.launch(ContextCoroutineName + xhandler, coroutineStart) {
        val result = ContextRef(ctx).handler()
        if (result != ctx) {
          ctx.render(result)
        }
      }
    }.handle(handler)
  }

  companion object {
    private val ContextCoroutineName = CoroutineName("ctx")
  }
}

/**
 * Creates and run jooby application.
 *
 * <pre>
 * run(*args) {
 *  get("/") {-> "Hi Kotlin"}
 * }
 * </pre>
 */
@RouterDsl
fun run(mode: ExecutionMode, vararg args: String, init: Kooby.() -> Unit) {
  Jooby.run({ Kooby(init) }, mode, args)
}

@RouterDsl
fun run(vararg args: String, init: Kooby.() -> Unit) {
  Jooby.run({ Kooby(init) }, args)
}

@RouterDsl
fun run(supplier: () -> Jooby, vararg args: String) {
  Jooby.run(supplier, ExecutionMode.DEFAULT, args)
}

@RouterDsl
fun run(supplier: () -> Jooby, mode: ExecutionMode, vararg args: String) {
  Jooby.run(supplier, mode, args)
}
