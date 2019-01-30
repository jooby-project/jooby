package io.jooby

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPEALIAS, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class RouterDsl

open class Kooby constructor() : Jooby() {
  constructor(init: Kooby.() -> Unit) : this() {
    this.init()
  }

  /**
   * Builds a route to match `GET` requests with specified [pattern]
   */
  @RouterDsl
  fun get(pattern: String = "/", init: Context.() -> Any): Route {
    return get(pattern) { ctx -> ctx.init() }.handle(init)
  }

  @RouterDsl
  fun post(pattern: String = "/", init: Context.() -> Any): Route {
    return post(pattern) { ctx -> ctx.init() }.handle(init)
  }

  @RouterDsl
  fun put(pattern: String = "/", init: Context.() -> Any): Route {
    return put(pattern) { ctx -> ctx.init() }.handle(init)
  }

  @RouterDsl
  fun delete(pattern: String = "/", init: Context.() -> Any): Route {
    return delete(pattern) { ctx -> ctx.init() }.handle(init)
  }

  @RouterDsl
  fun patch(pattern: String = "/", init: Context.() -> Any): Route {
    return patch(pattern) { ctx -> ctx.init() }.handle(init)
  }

  @RouterDsl
  fun head(pattern: String = "/", init: Context.() -> Any): Route {
    return head(pattern) { ctx -> ctx.init() }.handle(init)
  }

  @RouterDsl
  fun trace(pattern: String = "/", init: Context.() -> Any): Route {
    return trace(pattern) { ctx -> ctx.init() }.handle(init)
  }

  @RouterDsl
  fun options(pattern: String = "/", init: Context.() -> Any): Route {
    return options(pattern) { ctx -> ctx.init() }.handle(init)
  }

  @RouterDsl
  fun connect(pattern: String = "/", init: Context.() -> Any): Route {
    return connect(pattern) { ctx -> ctx.init() }.handle(init)
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
fun run(mode: ExecutionMode, args: Array<String>, init: Kooby.() -> Unit) {
  Jooby.run({ Kooby(init) }, mode, args)
}

@RouterDsl
fun run(args: Array<String>, init: Kooby.() -> Unit) {
  Jooby.run({ Kooby(init) }, args)
}

@RouterDsl
fun run(supplier: () -> Jooby, args: Array<String>) {
  Jooby.run(supplier, ExecutionMode.DEFAULT, args)
}

@RouterDsl
fun run(supplier: () -> Jooby, mode: ExecutionMode, args: Array<String>) {
  Jooby.run(supplier, mode, args)
}
