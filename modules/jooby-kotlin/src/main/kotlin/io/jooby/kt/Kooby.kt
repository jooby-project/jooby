/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE", "NOTHING_TO_INLINE")

package io.jooby.kt

import io.jooby.Body
import io.jooby.Context
import io.jooby.Cors
import io.jooby.Environment
import io.jooby.EnvironmentOptions
import io.jooby.ExecutionMode
import io.jooby.Formdata
import io.jooby.Jooby
import io.jooby.QueryString
import io.jooby.Registry
import io.jooby.Route
import io.jooby.RouteSet
import io.jooby.Router
import io.jooby.RouterOption
import io.jooby.ServerOptions
import io.jooby.ServiceRegistry
import io.jooby.Value
import io.jooby.ValueNode
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlinx.coroutines.CoroutineStart

@DslMarker
@Target(
  AnnotationTarget.CLASS,
  AnnotationTarget.TYPEALIAS,
  AnnotationTarget.TYPE,
  AnnotationTarget.FUNCTION
)
annotation class RouterDsl

@DslMarker
@Target(
  AnnotationTarget.CLASS,
  AnnotationTarget.TYPEALIAS,
  AnnotationTarget.TYPE,
  AnnotationTarget.FUNCTION
)
annotation class OptionsDsl

/** Registry: */
inline fun <reified T> Registry.require(): T {
  return this.require(T::class.java)
}

inline fun <reified T> Registry.require(name: String): T {
  return this.require(T::class.java, name)
}

fun <T : Any> Registry.require(klass: KClass<T>): T {
  return this.require(klass.java)
}

fun <T : Any> Registry.require(klass: KClass<T>, name: String): T {
  return this.require(klass.java, name)
}

fun <T : Any> ServiceRegistry.get(klass: KClass<T>): T {
  return this.get(klass.java)
}

fun <T : Any> ServiceRegistry.getOrNull(klass: KClass<T>): T? {
  return this.getOrNull(klass.java)
}

fun <T : Any> ServiceRegistry.put(klass: KClass<T>, service: T): T? {
  return this.put(klass.java, service)
}

fun <T : Any> ServiceRegistry.putIfAbsent(klass: KClass<T>, service: T): T? {
  return this.putIfAbsent(klass.java, service)
}

/** Value: */
inline operator fun <reified T> ValueNode.getValue(thisRef: Any?, property: KProperty<*>): T {
  return this.get(property.name).to(T::class.java)
}

inline fun <reified T> Value.to(): T {
  return this.to(T::class.java)
}

infix fun <T : Any> Value.to(type: KClass<T>): T {
  return this.to(type.java)
}

/** Context: */
val Context.query: QueryString
  get() = this.query()

val Context.form: Formdata
  get() = this.form()

val Context.body: Body
  get() = this.body()

inline fun <T : Any> Context.body(klass: KClass<T>): T {
  return this.body(klass.java)
}

inline fun <T : Any> Context.form(klass: KClass<T>): T {
  return this.form(klass.java)
}

inline fun <T : Any> Context.query(klass: KClass<T>): T {
  return this.query(klass.java)
}

/**
 * Welcome to Jooby for Kotlin
 *
 * <p>Hello World:
 * <pre>{@code
 *
 * import io.jooby.kt.runApp
 *
 * fun main(args: Array<String>) {
 *    runApp(args) {
 *      get ("/") { "Welcome to Jooby!" }
 *    }
 * }
 *
 * }</pre>
 *
 * More documentation at <a href="https://jooby.io">jooby.io</a>
 *
 * @author edgar
 * @since 2.0.0
 */
open class Kooby constructor() : Jooby() {

  constructor(init: Kooby.() -> Unit) : this() {
    this.init()
  }

  @RouterDsl
  fun <T : Any> mvc(router: KClass<T>): Kooby {
    super.mvc(router.java)
    return this
  }

  @RouterDsl
  fun <T : Any> mvc(router: KClass<T>, provider: () -> T): Kooby {
    super.mvc(router.java, provider)
    return this
  }

  @RouterDsl
  fun use(handler: FilterContext.() -> Any): Kooby {
    super.use { next -> Route.Handler { ctx -> FilterContext(ctx, next).handler() } }
    return this
  }

  @RouterDsl
  fun before(handler: HandlerContext.() -> Unit): Kooby {
    super.before { ctx -> HandlerContext(ctx).handler() }
    return this
  }

  @RouterDsl
  fun after(handler: AfterContext.() -> Unit): Kooby {
    super.after { ctx, result, failure -> AfterContext(ctx, result, failure).handler() }
    return this
  }

  @RouterDsl
  override fun path(pattern: String, action: Runnable): RouteSet {
    return super.path(pattern, action)
  }

  @RouterDsl
  override fun routes(action: Runnable): RouteSet {
    return super.routes(action)
  }

  @RouterDsl
  override fun get(pattern: String, handler: Route.Handler): Route {
    return super.get(pattern, handler)
  }

  @RouterDsl
  fun get(pattern: String, handler: HandlerContext.() -> Any): Route {
    return route(Router.GET, pattern, handler)
  }

  @RouterDsl
  override fun post(pattern: String, handler: Route.Handler): Route {
    return super.post(pattern, handler)
  }

  @RouterDsl
  fun post(pattern: String, handler: HandlerContext.() -> Any): Route {
    return route(Router.POST, pattern, handler)
  }

  @RouterDsl
  override fun put(pattern: String, handler: Route.Handler): Route {
    return super.put(pattern, handler)
  }

  @RouterDsl
  fun put(pattern: String, handler: HandlerContext.() -> Any): Route {
    return route(Router.PUT, pattern, handler)
  }

  @RouterDsl
  override fun delete(pattern: String, handler: Route.Handler): Route {
    return super.delete(pattern, handler)
  }

  @RouterDsl
  fun delete(pattern: String, handler: HandlerContext.() -> Any): Route {
    return route(Router.DELETE, pattern, handler)
  }

  @RouterDsl
  override fun patch(pattern: String, handler: Route.Handler): Route {
    return super.patch(pattern, handler)
  }

  @RouterDsl
  fun patch(pattern: String, handler: HandlerContext.() -> Any): Route {
    return route(Router.PATCH, pattern, handler)
  }

  @RouterDsl
  override fun head(pattern: String, handler: Route.Handler): Route {
    return super.head(pattern, handler)
  }

  @RouterDsl
  fun head(pattern: String, handler: HandlerContext.() -> Any): Route {
    return route(Router.HEAD, pattern, handler)
  }

  @RouterDsl
  override fun trace(pattern: String, handler: Route.Handler): Route {
    return super.trace(pattern, handler)
  }

  @RouterDsl
  fun trace(pattern: String, handler: HandlerContext.() -> Any): Route {
    return route(Router.TRACE, pattern, handler)
  }

  @RouterDsl
  override fun options(pattern: String, handler: Route.Handler): Route {
    return super.options(pattern, handler)
  }

  @RouterDsl
  fun options(pattern: String, handler: HandlerContext.() -> Any): Route {
    return route(Router.OPTIONS, pattern, handler)
  }

  @RouterDsl
  fun coroutine(
    coroutineStart: CoroutineStart = CoroutineStart.DEFAULT,
    block: CoroutineRouter.() -> Unit
  ): CoroutineRouter {
    val from = routes.size
    val router =
      attributes.computeIfAbsent("coroutineRouter") { CoroutineRouter(coroutineStart, this) }
        as CoroutineRouter
    router.block()
    routes.subList(from, routes.size).forEach {
      it.setNonBlocking(true)
      it.attribute("coroutine", true)
    }
    return router
  }

  @RouterDsl
  override fun route(method: String, pattern: String, handler: Route.Handler): Route {
    return super.route(method, pattern, handler)
  }

  @RouterDsl
  fun route(method: String, pattern: String, handler: HandlerContext.() -> Any): Route {
    return super.route(method, pattern) { ctx -> handler(HandlerContext(ctx)) }.setHandle(handler)
  }

  @RouterDsl
  fun ws(pattern: String, handler: WebSocketInitContext.() -> Any): Route {
    return super.ws(pattern) { ctx, initializer -> handler(WebSocketInitContext(ctx, initializer)) }
  }

  @RouterDsl
  fun sse(pattern: String, handler: ServerSentHandler.() -> Any): Route {
    return super.sse(pattern) { sse -> handler(ServerSentHandler(sse.context, sse)) }
  }

  @OptionsDsl
  fun serverOptions(configurer: ServerOptions.() -> Unit): Kooby {
    val options = ServerOptions()
    configurer(options)
    setServerOptions(options)
    return this
  }

  @OptionsDsl
  fun routerOptions(vararg option: RouterOption): Kooby {
    this.setRouterOptions(*option)
    return this
  }

  @OptionsDsl
  fun environmentOptions(configurer: EnvironmentOptions.() -> Unit): Environment {
    val options = EnvironmentOptions()
    configurer(options)
    val env = Environment.loadEnvironment(options)
    this.environment = env
    return env
  }
}

/** cors: */
@OptionsDsl
fun cors(init: Cors.() -> Unit): Cors {
  val cors = Cors()
  cors.init()
  return cors
}

/** runApp: */
fun runApp(args: Array<String>, mode: ExecutionMode, init: Kooby.() -> Unit) {
  configurePackage(init)
  Jooby.runApp(args, mode, fun() = Kooby(init))
}

fun runApp(args: Array<String>, init: Kooby.() -> Unit) {
  configurePackage(init)
  Jooby.runApp(args, ExecutionMode.DEFAULT, fun() = Kooby(init))
}

fun <T : Jooby> runApp(args: Array<String>, provider: () -> T) {
  runApp(args, ExecutionMode.DEFAULT, provider)
}

fun <T : Jooby> runApp(args: Array<String>, mode: ExecutionMode, provider: () -> T) {
  //  System.setProperty("___app_name__", application.java.simpleName)
  Jooby.runApp(args, mode, provider)
}

internal fun configurePackage(value: Any) {
  val appname = value::class.java.name
  val start = appname.indexOf(".").let { if (it == -1) 0 else it + 1 }

  val end = appname.indexOf("Kt$")
  System.setProperty("___app_name__", appname.substring(start, end))
  value::class.java.`package`?.let { System.setProperty("application.package", it.name) }
}
