package org.jooby

import kotlin.reflect.KClass
import org.jooby.spi.Server
import java.util.SortedSet

/**
 * Collection of utility class and method to make Jooby more Kotlin.
 */
class KRouteGroup(b: Route.Props<Route.Group>) : Route.Props<Route.Group> by b {
  private val g = b as Route.Group

  /** ALL */
  fun all(pattern: String, filter: (Request, Response, Route.Chain) -> Unit) {
    g.all(pattern, filter)
  }

  fun all(pattern: String, filter: (Request, Response) -> Unit) {
    g.all(pattern, filter)
  }

  fun all(filter: (Request, Response, Route.Chain) -> Unit) {
    g.all(filter)
  }

  fun all(filter: (Request, Response) -> Unit) {
    g.all(filter)
  }

  fun all(pattern: String, init: Request.() -> Any) {
    g.all(pattern, {req-> req.init()})
  }

  fun all(init: Request.() -> Any) {
    g.all({req-> req.init()})
  }

  /** GET */
  fun get(pattern: String, filter: (Request, Response, Route.Chain) -> Unit) {
    g.get(pattern, filter)
  }

  fun get(pattern: String, filter: (Request, Response) -> Unit) {
    g.get(pattern, filter)
  }

  fun get(filter: (Request, Response, Route.Chain) -> Unit) {
    g.get(filter)
  }

  fun get(filter: (Request, Response) -> Unit) {
    g.get(filter)
  }

  fun get(pattern: String, init: Request.() -> Any) {
    g.get(pattern, {req-> req.init()})
  }

  fun get(init: Request.() -> Any): Unit {
    g.get({req-> req.init()})
  }

  /** POST */
  fun post(pattern: String, filter: (Request, Response, Route.Chain) -> Unit) {
    g.post(pattern, filter)
  }

  fun post(pattern: String, filter: (Request, Response) -> Unit) {
    g.post(pattern, filter)
  }

  fun post(filter: (Request, Response, Route.Chain) -> Unit) {
    g.post(filter)
  }

  fun post(filter: (Request, Response) -> Unit) {
    g.post(filter)
  }

  fun post(pattern: String, init: Request.() -> Any) {
    g.post(pattern, {req-> req.init()})
  }

  fun post(init: Request.() -> Any) {
    g.post({req-> req.init()})
  }

  /** PUT */
  fun put(pattern: String, filter: (Request, Response, Route.Chain) -> Unit) {
    g.put(pattern, filter)
  }

  fun put(pattern: String, filter: (Request, Response) -> Unit) {
    g.put(pattern, filter)
  }

  fun put(filter: (Request, Response, Route.Chain) -> Unit) {
    g.put(filter)
  }

  fun put(filter: (Request, Response) -> Unit) {
    g.put(filter)
  }

  fun put(pattern: String, init: Request.() -> Any) {
    g.put(pattern, {req-> req.init()})
  }

  fun put(init: Request.() -> Any) {
    g.put({req-> req.init()})
  }

  /** PATCH */
  fun patch(pattern: String, filter: (Request, Response, Route.Chain) -> Unit) {
    g.patch(pattern, filter)
  }

  fun patch(pattern: String, filter: (Request, Response) -> Unit) {
    g.patch(pattern, filter)
  }

  fun patch(filter: (Request, Response, Route.Chain) -> Unit) {
    g.patch(filter)
  }

  fun patch(filter: (Request, Response) -> Unit) {
    g.patch(filter)
  }

  fun patch(pattern: String, init: Request.() -> Any) {
    g.patch(pattern, {req-> req.init()})
  }

  fun patch(init: Request.() -> Any) {
    g.patch({req-> req.init()})
  }

  /** DELETE */
  fun delete(pattern: String, filter: (Request, Response, Route.Chain) -> Unit) {
    g.delete(pattern, filter)
  }

  fun delete(pattern: String, filter: (Request, Response) -> Unit) {
    g.delete(pattern, filter)
  }

  fun delete(filter: (Request, Response, Route.Chain) -> Unit) {
    g.delete(filter)
  }

  fun delete(filter: (Request, Response) -> Unit) {
    g.delete(filter)
  }

  fun delete(pattern: String, init: Request.() -> Any) {
    g.delete(pattern, {req-> req.init()})
  }
  
  fun delete(init: Request.() -> Any) {
    g.delete({req-> req.init()})
  }
}

/**
  * Collection of utility class and method to make Jooby more Kotlin.
  */
open class Kooby: Jooby() {
  fun <T:Any> use(klass: KClass<T>): Route.Collection {
    return use(klass.java)
  }

  fun <T:Session.Store> session(klass: KClass<T>): Session.Definition {
    return session(klass.java)
  }

  fun <T:Server> server(klass: KClass<T>): Jooby {
    return server(klass.java)
  }

  /**
   * Extension function that replace the Jooby#use(java.lang.String) function with a more appropiated
   * name.
   *
   * @param pattern Route group pattern.
   * @param init Group Initializer.
   */
  fun route(pattern: String, init: KRouteGroup.() -> Unit): KRouteGroup {
    val group = KRouteGroup(this.use(pattern))
    group.init()
    return group
  }

  fun get(pattern: String = "/", init: Request.() -> Any): Route.Definition {
    return get(pattern, {req-> req.init()})
  }

  fun post(pattern: String = "/", init: Request.() -> Any): Route.Definition {
    return post(pattern, {req-> req.init()})
  }

  fun put(pattern: String = "/", init: Request.() -> Any): Route.Definition {
    return put(pattern, {req-> req.init()})
  }

  fun patch(pattern: String = "/", init: Request.() -> Any): Route.Definition {
    return patch(pattern, {req-> req.init()})
  }

  fun delete(pattern: String = "/", init: Request.() -> Any): Route.Definition {
    return delete(pattern, {req-> req.init()})
  }

  fun trace(pattern: String = "/", init: Request.() -> Any): Route.Definition {
    return trace(pattern, {req-> req.init()})
  }

  fun connect(pattern: String = "/", init: Request.() -> Any): Route.Definition {
    return connect(pattern, {req-> req.init()})
  }

  fun options(pattern: String = "/", init: Request.() -> Any): Route.Definition {
    return options(pattern, {req-> req.init()})
  }

  fun head(pattern: String = "/", init: Request.() -> Any): Route.Definition {
    return head(pattern, {req-> req.init()})
  }
}

/**
 * Creates jooby application and return it.
 *
 * <pre>
 * val app = jooby {
 *  get("/") {-> "Hi Kotlin"}
 * }
 * </pre>
 */
fun jooby(init: Kooby.() -> Unit): Jooby {
  val app = Kooby()
  app.init()
  return app
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
fun run(vararg args: String, init: Kooby.() -> Unit): Unit {
  Jooby.run({ ->
    val app = Kooby()
    app.init()
    app
  }, args)
}

// Redefine functions with class arguments
fun <T:Throwable> Router.err(klass: KClass<T>, handler: Err.Handler): Router {
    return this.err(klass.java, handler)
  }

fun <T:Any> Registry.require(klass: KClass<T>): T {
  return this.require(klass.java)
}

fun <T:Any> Registry.require(name: String, klass: KClass<T>): T {
  return this.require(name, klass.java)
}

// *********************************** LifeCycle **************************************************
fun <T:Any> LifeCycle.lifeCycle(klass: KClass<T>): LifeCycle {
  return this.lifeCycle(klass.java)
}

// *********************************** Mutant *****************************************************
val Mutant.booleanValue: Boolean
  get() = booleanValue()

val Mutant.shortValue: Short
  get() = shortValue()

val Mutant.charValue: Char
  get() = charValue()

val Mutant.byteValue: Byte
  get() = byteValue()

val Mutant.intValue: Int
  get() = intValue()

val Mutant.longValue: Long
  get() = longValue()

val Mutant.floatValue: Float
  get() = floatValue()

val Mutant.doubleValue: Double
  get() = doubleValue()

val Mutant.value: String
  get() = value()

fun <T:Any> Mutant.to(type: KClass<T>): T {
  return this.to(type.java)
}

fun <T:Any> Mutant.to(type: KClass<T>, contentType: MediaType): T {
  return this.to(type.java, contentType)
}

fun <T:Any> Mutant.to(type: KClass<T>, contentType: String): T {
  return this.to(type.java, contentType)
}

fun <T:Enum<T>> Mutant.toEnum(type: KClass<T>): T {
  return this.toEnum(type.java)
}

fun <T:Any> Mutant.toOptional(type: KClass<T>): java.util.Optional<T> {
  return this.toOptional(type.java)
}

fun <T:Any> Mutant.toList(type: KClass<T>): List<T> {
  return this.toList(type.java)
}

fun <T:Any> Mutant.toSet(type: KClass<T>): Set<T> {
  return this.toSet(type.java)
}

fun <T:Comparable<T>> Mutant.toSortedSet(type: KClass<T>): SortedSet<T> {
  return this.toSortedSet(type.java)
}

// *********************************** Request *****************************************************
fun <T:Any> Request.set(type: KClass<T>, value: Any): Request {
  return this.set(type.java, value)
}
