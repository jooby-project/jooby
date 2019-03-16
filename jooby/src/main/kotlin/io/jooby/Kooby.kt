package io.jooby

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPEALIAS, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class RouterDsl

/** Value: */
operator fun Value.get(name: String): Value {
  return this.get(name)
}

operator fun Value.get(index: Int): Value {
  return this.get(index)
}

inline fun <reified T : Any> Value.to(): T {
  val reified = object : Reified<T>() {}
  return this.to(reified)
}

/** Context: */
inline fun <reified T : Any> Context.query(): T {
  val reified = object : Reified<T>() {}
  return this.query(reified)
}

inline fun <reified T : Any> Context.form(): T {
  val reified = object : Reified<T>() {}
  return this.form(reified)
}

inline fun <reified T : Any> Context.multipart(): T {
  val reified = object : Reified<T>() {}
  return this.multipart(reified)
}

inline fun <reified T : Any> Context.body(): T {
  val reified = object : Reified<T>() {}
  return this.body(reified)
}

/** Handler context: */
class AfterContext(val ctx: Context, val result: Any)

class HandlerContext(val ctx: Context)

class DecoratorContext(val ctx: Context, val next: Route.Handler)

/** Kooby: */

internal class WorkerCoroutineScope(coroutineContext: CoroutineContext) : CoroutineScope {
  override val coroutineContext = coroutineContext
}

open class Kooby constructor() : Jooby() {

  var coroutineStart: CoroutineStart = CoroutineStart.DEFAULT

  private val coroutineScope: CoroutineScope by lazy {
    WorkerCoroutineScope(worker().asCoroutineDispatcher())
  }

  constructor(init: Kooby.() -> Unit) : this() {
    this.init()
  }

  fun <T:Any> mvc(router: KClass<T>): Kooby {
    super.mvc(router.java)
    return this
  }

  @RouterDsl
  fun decorator(handler: DecoratorContext.() -> Any): Kooby {
    super.decorator { next -> Route.Handler { ctx -> DecoratorContext(ctx, next).handler() } }
    return this
  }

  @RouterDsl
  fun before(handler: HandlerContext.() -> Unit): Kooby {
    super.before { ctx -> HandlerContext(ctx).handler() }
    return this
  }

  @RouterDsl
  fun after(handler: AfterContext.() -> Any): Kooby {
    super.after { ctx, result -> AfterContext(ctx, result).handler() }
    return this
  }

  @RouterDsl
  fun get(pattern: String = "/", handler: suspend HandlerContext.() -> Any): Route {
    return route(Router.GET, pattern, handler)
  }

  @RouterDsl
  override fun get(pattern: String, handler: Route.Handler): Route {
    return super.get(pattern, handler)
  }

  @RouterDsl
  fun post(pattern: String = "/", handler: suspend HandlerContext.() -> Any): Route {
    return route(Router.POST, pattern, handler)
  }

  @RouterDsl
  override fun post(pattern: String, handler: Route.Handler): Route {
    return super.post(pattern, handler)
  }

  @RouterDsl
  fun put(pattern: String = "/", handler: suspend HandlerContext.() -> Any): Route {
    return route(Router.PUT, pattern, handler)
  }

  @RouterDsl
  override fun put(pattern: String, handler: Route.Handler): Route {
    return super.put(pattern, handler)
  }

  @RouterDsl
  fun delete(pattern: String = "/", handler: suspend HandlerContext.() -> Any): Route {
    return route(Router.DELETE, pattern, handler)
  }

  @RouterDsl
  override fun delete(pattern: String, handler: Route.Handler): Route {
    return super.delete(pattern, handler)
  }

  @RouterDsl
  fun patch(pattern: String = "/", handler: suspend HandlerContext.() -> Any): Route {
    return route(Router.PATCH, pattern, handler)
  }

  @RouterDsl
  override fun patch(pattern: String, handler: Route.Handler): Route {
    return super.patch(pattern, handler)
  }

  @RouterDsl
  fun head(pattern: String = "/", handler: suspend HandlerContext.() -> Any): Route {
    return route(Router.HEAD, pattern, handler)
  }

  @RouterDsl
  override fun head(pattern: String, handler: Route.Handler): Route {
    return super.head(pattern, handler)
  }

  @RouterDsl
  fun trace(pattern: String = "/", handler: suspend HandlerContext.() -> Any): Route {
    return route(Router.TRACE, pattern, handler)
  }

  @RouterDsl
  override fun trace(pattern: String, handler: Route.Handler): Route {
    return super.trace(pattern, handler)
  }

  @RouterDsl
  fun options(pattern: String = "/", handler: suspend HandlerContext.() -> Any): Route {
    return route(Router.OPTIONS, pattern, handler)
  }

  @RouterDsl
  override fun options(pattern: String, handler: Route.Handler): Route {
    return super.options(pattern, handler)
  }

  @RouterDsl
  fun connect(pattern: String = "/", handler: suspend HandlerContext.() -> Any): Route {
    return route(Router.CONNECT, pattern, handler)
  }

  @RouterDsl
  override fun connect(pattern: String, handler: Route.Handler): Route {
    return super.connect(pattern, handler)
  }

  @RouterDsl
  fun route(method: String, pattern: String, handler: suspend HandlerContext.() -> Any): Route {
    return route(method, pattern) { ctx ->
      val xhandler = CoroutineExceptionHandler { _, x ->
        ctx.sendError(x)
      }
      coroutineScope.launch(ContextCoroutineName + xhandler, coroutineStart) {
        val result = HandlerContext(ctx).handler()
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
  configurePackage(init)
  Jooby.run({ Kooby(init) }, mode, args)
}

@RouterDsl
fun run(args: Array<String>, init: Kooby.() -> Unit) {
  configurePackage(init)
  Jooby.run({ Kooby(init) }, ExecutionMode.DEFAULT, args)
}

// ::App
@RouterDsl
fun run(init: () -> Kooby, args: Array<String>) {
  run(init, ExecutionMode.DEFAULT, args)
}

@RouterDsl
fun run(init: () -> Kooby, mode: ExecutionMode, args: Array<String>) {
  configurePackage(init)
  Jooby.run(init, mode, args)
}

internal fun configurePackage(value: Any) {
  value::class.java.`package`?.let { System.setProperty(Jooby.DEF_PCKG, it.name) }
}
