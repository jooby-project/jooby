/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby

import kotlinx.coroutines.CoroutineStart
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPEALIAS, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class RouterDsl

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPEALIAS, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
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

operator fun ValueNode.get(name: String): ValueNode {
  return this.get(name)
}

operator fun ValueNode.get(index: Int): ValueNode {
  return this.get(index)
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

inline fun <reified T> Context.query(): T {
  return this.query(T::class.java)
}

val Context.form: Formdata
  get() = this.form()

inline fun <reified T> Context.form(): T {
  return this.form(T::class.java)
}

val Context.multipart: Multipart
  get() = this.multipart()

inline fun <reified T> Context.multipart(): T {
  return this.multipart(T::class.java)
}

val Context.body: Body
  get() = this.body()

inline fun <reified T> Context.body(): T {
  return this.body(T::class.java)
}

/** Kooby: */
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
    super.after { ctx, result, failure -> AfterContext(ctx, result, failure).handler() }
    return this
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
  fun coroutine(coroutineStart: CoroutineStart = CoroutineStart.DEFAULT, block: CoroutineRouter.() -> Unit): CoroutineRouter {
    val router = attributes.computeIfAbsent("coroutineRouter") { CoroutineRouter(coroutineStart, this) } as CoroutineRouter
    router.block()
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

fun <T : Jooby> runApp(args: Array<String>, application: KClass<T>) {
  runApp(args, ExecutionMode.DEFAULT, application)
}

fun <T : Jooby> runApp(args: Array<String>, mode: ExecutionMode, application: KClass<T>) {
  Jooby.runApp(args, mode, application.java)
}

internal fun configurePackage(value: Any) {
  value::class.java.`package`?.let { System.setProperty(Jooby.BASE_PACKAGE, it.name) }
}
