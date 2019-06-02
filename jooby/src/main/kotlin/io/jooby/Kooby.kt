/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
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

/** Registry: */
inline fun <reified T> Registry.require(): T {
  return this.require(T::class.java)
}

inline fun <reified T> Registry.require(name: String): T {
  return this.require(T::class.java, name)
}

fun <T:Any> Registry.require(klass: KClass<T>): T {
  return this.require(klass.java)
}

fun <T:Any> Registry.require(klass: KClass<T>, name: String): T {
  return this.require(klass.java, name)
}

fun <T:Any> ServiceRegistry.get(klass: KClass<T>): T {
  return this.get(klass.java)
}

fun <T:Any> ServiceRegistry.put(klass: KClass<T>, service: T): T? {
  return this.put(klass.java, service)
}

fun <T:Any> ServiceRegistry.putIfAbsent(klass: KClass<T>, service: T): T? {
  return this.putIfAbsent(klass.java, service)
}

/** Value: */
operator fun Value.get(name: String): Value {
  return this.get(name)
}

operator fun Value.get(index: Int): Value {
  return this.get(index)
}

inline fun <reified T> Value.to(): T {
  return this.to(object : Reified<T>() {})
}

/** Context: */
inline fun <reified T> Context.query(): T {
  val reified = object : Reified<T>() {}
  return this.query(reified)
}

inline fun <reified T> Context.form(): T {
  return this.form(object : Reified<T>() {})
}

inline fun <reified T> Context.multipart(): T {
  return this.multipart(object : Reified<T>() {})
}

inline fun <reified T> Context.body(): T {
  return this.body(object : Reified<T>() {})
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

  val coroutineScope: CoroutineScope by lazy {
    WorkerCoroutineScope(getWorker().asCoroutineDispatcher())
  }

  constructor(init: Kooby.() -> Unit) : this() {
    this.init()
  }

  fun <T : Any> mvc(router: KClass<T>): Kooby {
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
    }.setHandle(handler)
  }

  @RouterDsl
  override fun route(method: String, pattern: String, handler: Route.Handler): Route {
    return super.route(method, pattern, handler)
  }

  fun serverOptions(configurer: ServerOptions.() -> Unit): Kooby {
    val options = ServerOptions()
    configurer(options)
    setServerOptions(options)
    return this
  }

  fun routerOptions(configurer: RouterOptions.() -> Unit): Kooby {
    val options = RouterOptions()
    configurer(options)
    this.routerOptions = options
    return this
  }

  fun environmentOptions(configurer: EnvironmentOptions.() -> Unit): Environment {
    val options = EnvironmentOptions()
    configurer(options)
    val env = Environment.loadEnvironment(options)
    environment = env
    return env
  }

  companion object {
    private val ContextCoroutineName = CoroutineName("ctx")
  }
}

@RouterDsl
fun runApp(args: Array<String>, mode: ExecutionMode, init: Kooby.() -> Unit) {
  configurePackage(init)
  Jooby.runApp(args, mode, fun() = Kooby(init))
}

@RouterDsl
fun runApp(args: Array<String>, init: Kooby.() -> Unit) {
  configurePackage(init)
  Jooby.runApp(args, ExecutionMode.DEFAULT, fun() = Kooby(init))
}

@RouterDsl
fun <T : Jooby> runApp(args: Array<String>, application: KClass<T>) {
  runApp(args, ExecutionMode.DEFAULT, application)
}

@RouterDsl
fun <T : Jooby> runApp(args: Array<String>, mode: ExecutionMode, application: KClass<T>) {
  Jooby.runApp(args, mode, application.java)
}

internal fun configurePackage(value: Any) {
  value::class.java.`package`?.let { System.setProperty(Jooby.BASE_PACKAGE, it.name) }
}
