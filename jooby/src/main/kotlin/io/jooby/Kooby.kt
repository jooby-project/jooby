package io.jooby

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPEALIAS, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class RouterDsl

internal class JoobyCoroutineScope(coroutineContext: CoroutineContext) : CoroutineScope {
  override val coroutineContext = coroutineContext
}

class ContextRef(val ctx: Context)

open class Kooby constructor() : Jooby() {

  val workerScope: CoroutineScope by lazy {
    val dispatcher: CoroutineDispatcher = worker().asCoroutineDispatcher()
    JoobyCoroutineScope(dispatcher)
  }

  constructor(init: Kooby.() -> Unit) : this() {
    this.init()
  }

  @RouterDsl
  @ExperimentalCoroutinesApi
  fun get(pattern: String = "/",
          coroutineScope: CoroutineScope? = null,
          handler: suspend ContextRef.() -> Any): Route {
    return route(Router.GET, pattern, coroutineScope, handler)
  }

  @RouterDsl
  @ExperimentalCoroutinesApi
  fun post(pattern: String = "/",
          coroutineScope: CoroutineScope? = null,
          handler: suspend ContextRef.() -> Any): Route {
    return route(Router.POST, pattern, coroutineScope, handler)
  }

  @RouterDsl
  @ExperimentalCoroutinesApi
  fun put(pattern: String = "/",
           coroutineScope: CoroutineScope? = null,
           handler: suspend ContextRef.() -> Any): Route {
    return route(Router.PUT, pattern, coroutineScope, handler)
  }

  @RouterDsl
  @ExperimentalCoroutinesApi
  fun delete(pattern: String = "/",
          coroutineScope: CoroutineScope? = null,
          handler: suspend ContextRef.() -> Any): Route {
    return route(Router.DELETE, pattern, coroutineScope, handler)
  }

  @RouterDsl
  @ExperimentalCoroutinesApi
  fun patch(pattern: String = "/",
             coroutineScope: CoroutineScope? = null,
             handler: suspend ContextRef.() -> Any): Route {
    return route(Router.PATCH, pattern, coroutineScope, handler)
  }

  @RouterDsl
  @ExperimentalCoroutinesApi
  fun head(pattern: String = "/",
            coroutineScope: CoroutineScope? = null,
            handler: suspend ContextRef.() -> Any): Route {
    return route(Router.HEAD, pattern, coroutineScope, handler)
  }

  @RouterDsl
  @ExperimentalCoroutinesApi
  fun trace(pattern: String = "/",
            coroutineScope: CoroutineScope? = null,
            handler: suspend ContextRef.() -> Any): Route {
    return route(Router.TRACE, pattern, coroutineScope, handler)
  }

  @RouterDsl
  @ExperimentalCoroutinesApi
  fun options(pattern: String = "/",
            coroutineScope: CoroutineScope? = null,
            handler: suspend ContextRef.() -> Any): Route {
    return route(Router.OPTIONS, pattern, coroutineScope, handler)
  }

  @RouterDsl
  @ExperimentalCoroutinesApi
  fun connect(pattern: String = "/",
              coroutineScope: CoroutineScope? = null,
              handler: suspend ContextRef.() -> Any): Route {
    return route(Router.CONNECT, pattern, coroutineScope, handler)
  }

  @RouterDsl
  @ExperimentalCoroutinesApi
  fun route(method: String, pattern: String,
            coroutineScope: CoroutineScope? = null,
            handler: suspend ContextRef.() -> Any): Route {
    return route(method, pattern) { ctx ->
      val xhandler = CoroutineExceptionHandler { _, x ->
        ctx.sendError(x)
      }
      (coroutineScope ?: workerScope).launch(xhandler) {
        val result = ContextRef(ctx).handler()
        if (result != ctx) {
          ctx.render(result)
        }
      }
    }.handle(handler)
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
