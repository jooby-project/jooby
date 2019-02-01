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

  private val coroutineScope: CoroutineScope = WorkerCoroutineScope(worker().asCoroutineDispatcher())

  constructor(init: Kooby.() -> Unit) : this() {
    this.init()
  }

  @RouterDsl
  fun get(pattern: String = "/", handler: suspend ContextRef.() -> Any): Route {
    return get(pattern, coroutineScope, handler)
  }

  @RouterDsl
  fun get(pattern: String = "/", coroutineScope: CoroutineScope, handler: suspend ContextRef.() -> Any): Route {
    return route(Router.GET, pattern, coroutineScope, handler)
  }

  @RouterDsl
  fun post(pattern: String = "/", handler: suspend ContextRef.() -> Any): Route {
    return post(pattern, coroutineScope, handler)
  }

  @RouterDsl
  fun post(pattern: String = "/", coroutineScope: CoroutineScope, handler: suspend ContextRef.() -> Any): Route {
    return route(Router.POST, pattern, coroutineScope, handler)
  }

  @RouterDsl
  fun put(pattern: String = "/", handler: suspend ContextRef.() -> Any): Route {
    return put(pattern, coroutineScope, handler)
  }

  @RouterDsl
  fun put(pattern: String = "/", coroutineScope: CoroutineScope, handler: suspend ContextRef.() -> Any): Route {
    return route(Router.PUT, pattern, coroutineScope, handler)
  }

  @RouterDsl
  fun delete(pattern: String = "/", handler: suspend ContextRef.() -> Any): Route {
    return delete(pattern, coroutineScope, handler)
  }

  @RouterDsl
  fun delete(pattern: String = "/", coroutineScope: CoroutineScope, handler: suspend ContextRef.() -> Any): Route {
    return route(Router.DELETE, pattern, coroutineScope, handler)
  }

  @RouterDsl
  fun patch(pattern: String = "/", handler: suspend ContextRef.() -> Any): Route {
    return patch(pattern, coroutineScope, handler)
  }

  @RouterDsl
  fun patch(pattern: String = "/", coroutineScope: CoroutineScope, handler: suspend ContextRef.() -> Any): Route {
    return route(Router.PATCH, pattern, coroutineScope, handler)
  }

  @RouterDsl
  fun head(pattern: String = "/", handler: suspend ContextRef.() -> Any): Route {
    return head(pattern, coroutineScope, handler)
  }

  @RouterDsl
  fun head(pattern: String = "/", coroutineScope: CoroutineScope, handler: suspend ContextRef.() -> Any): Route {
    return route(Router.HEAD, pattern, coroutineScope, handler)
  }

  @RouterDsl
  fun trace(pattern: String = "/", handler: suspend ContextRef.() -> Any): Route {
    return trace(pattern, coroutineScope, handler)
  }

  @RouterDsl
  fun trace(pattern: String = "/", coroutineScope: CoroutineScope, handler: suspend ContextRef.() -> Any): Route {
    return route(Router.TRACE, pattern, coroutineScope, handler)
  }

  @RouterDsl
  fun options(pattern: String = "/", handler: suspend ContextRef.() -> Any): Route {
    return options(pattern, coroutineScope, handler)
  }

  @RouterDsl
  fun options(pattern: String = "/", coroutineScope: CoroutineScope, handler: suspend ContextRef.() -> Any): Route {
    return route(Router.OPTIONS, pattern, coroutineScope, handler)
  }

  @RouterDsl
  fun connect(pattern: String = "/", handler: suspend ContextRef.() -> Any): Route {
    return connect(pattern, coroutineScope, handler)
  }

  @RouterDsl
  fun connect(pattern: String = "/", coroutineScope: CoroutineScope, handler: suspend ContextRef.() -> Any): Route {
    return route(Router.CONNECT, pattern, coroutineScope, handler)
  }

  @RouterDsl
  fun route(method: String, pattern: String, handler: suspend ContextRef.() -> Any): Route {
    return route(method, pattern, coroutineScope, handler)
  }

  @RouterDsl
  fun route(method: String, pattern: String, coroutineScope: CoroutineScope,
            handler: suspend ContextRef.() -> Any): Route {
    return route(method, pattern) { ctx ->
      val xhandler = CoroutineExceptionHandler { _, x ->
        ctx.sendError(x)
      }
      coroutineScope.launch(ContextCoroutineName + xhandler) {
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
