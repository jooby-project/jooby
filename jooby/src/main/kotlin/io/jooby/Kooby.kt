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

internal val NO_ARG = arrayOf<String>()

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
  override fun get(pattern: String, handler: Route.Handler): Route {
    return super.get(pattern, handler)
  }

  @RouterDsl
  fun post(pattern: String = "/", handler: suspend ContextRef.() -> Any): Route {
    return route(Router.POST, pattern, handler)
  }

  @RouterDsl
  override fun post(pattern: String, handler: Route.Handler): Route {
    return super.post(pattern, handler)
  }

  @RouterDsl
  fun put(pattern: String = "/", handler: suspend ContextRef.() -> Any): Route {
    return route(Router.PUT, pattern, handler)
  }

  @RouterDsl
  override fun put(pattern: String, handler: Route.Handler): Route {
    return super.put(pattern, handler)
  }

  @RouterDsl
  fun delete(pattern: String = "/", handler: suspend ContextRef.() -> Any): Route {
    return route(Router.DELETE, pattern, handler)
  }

  @RouterDsl
  override fun delete(pattern: String, handler: Route.Handler): Route {
    return super.delete(pattern, handler)
  }

  @RouterDsl
  fun patch(pattern: String = "/", handler: suspend ContextRef.() -> Any): Route {
    return route(Router.PATCH, pattern, handler)
  }

  @RouterDsl
  override fun patch(pattern: String, handler: Route.Handler): Route {
    return super.patch(pattern, handler)
  }

  @RouterDsl
  fun head(pattern: String = "/", handler: suspend ContextRef.() -> Any): Route {
    return route(Router.HEAD, pattern, handler)
  }

  @RouterDsl
  override fun head(pattern: String, handler: Route.Handler): Route {
    return super.head(pattern, handler)
  }

  @RouterDsl
  fun trace(pattern: String = "/", handler: suspend ContextRef.() -> Any): Route {
    return route(Router.TRACE, pattern, handler)
  }

  @RouterDsl
  override fun trace(pattern: String, handler: Route.Handler): Route {
    return super.trace(pattern, handler)
  }

  @RouterDsl
  fun options(pattern: String = "/", handler: suspend ContextRef.() -> Any): Route {
    return route(Router.OPTIONS, pattern, handler)
  }

  @RouterDsl
  override fun options(pattern: String, handler: Route.Handler): Route {
    return super.options(pattern, handler)
  }

  @RouterDsl
  fun connect(pattern: String = "/", handler: suspend ContextRef.() -> Any): Route {
    return route(Router.CONNECT, pattern, handler)
  }

  @RouterDsl
  override fun connect(pattern: String, handler: Route.Handler): Route {
    return super.connect(pattern, handler)
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

  @RouterDsl
  override fun route(method: String, pattern: String, handler: Route.Handler): Route {
    return super.route(method, pattern, handler)
  }

  companion object {
    private val ContextCoroutineName = CoroutineName("ctx")
  }
}

@RouterDsl
fun run(mode: ExecutionMode, args: Array<String>, init: Kooby.() -> Unit) {
  Jooby.run({ Kooby(init) }, mode, args)
}

@RouterDsl
fun run(args: Array<String>, init: Kooby.() -> Unit) {
  Jooby.run({ Kooby(init) }, ExecutionMode.DEFAULT, args)
}

// ::App
@RouterDsl
fun run(supplier: () -> Kooby, args: Array<String>) {
  run(supplier, ExecutionMode.DEFAULT, args)
}

@RouterDsl
fun run(supplier: () -> Kooby, mode: ExecutionMode, args: Array<String>) {
  Jooby.run(supplier, mode, args)
}

// jooby {..}
fun jooby(init: Jooby.() -> Unit) {
  jooby(NO_ARG, init)
}

fun jooby(args: Array<String>, init: Jooby.() -> Unit) {
  jooby(ExecutionMode.DEFAULT, args, init)
}

fun jooby(mode: ExecutionMode, args: Array<String>, init: Jooby.() -> Unit) {
  Jooby.run({
    val app = Jooby()
    app.init()
    app
  }, mode, args)
}
