package io.jooby

open class Kooby constructor() : Jooby() {
  constructor(init: Kooby.() -> Unit) : this() {
    this.init()
  }

  fun get(pattern: String = "/", init: Context.() -> Any): Route {
    return get(pattern) { ctx -> ctx.init() }.handle(init)
  }

  fun post(pattern: String = "/", init: Context.() -> Any): Route {
    return post(pattern) { ctx -> ctx.init() }.handle(init)
  }

  fun put(pattern: String = "/", init: Context.() -> Any): Route {
    return put(pattern) { ctx -> ctx.init() }.handle(init)
  }

  fun delete(pattern: String = "/", init: Context.() -> Any): Route {
    return delete(pattern) { ctx -> ctx.init() }.handle(init)
  }

  fun patch(pattern: String = "/", init: Context.() -> Any): Route {
    return patch(pattern) { ctx -> ctx.init() }.handle(init)
  }

  fun head(pattern: String = "/", init: Context.() -> Any): Route {
    return head(pattern) { ctx -> ctx.init() }.handle(init)
  }

  fun trace(pattern: String = "/", init: Context.() -> Any): Route {
    return trace(pattern) { ctx -> ctx.init() }.handle(init)
  }

  fun options(pattern: String = "/", init: Context.() -> Any): Route {
    return options(pattern) { ctx -> ctx.init() }.handle(init)
  }

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
fun run(mode: ExecutionMode, args: Array<String>, init: Kooby.() -> Unit) {
  Jooby.run({ Kooby(init) }, mode, args)
}

fun run(args: Array<String>, init: Kooby.() -> Unit) {
  Jooby.run({ Kooby(init) }, args)
}

fun run(supplier: () -> Jooby, args: Array<String>) {
  Jooby.run(supplier, ExecutionMode.DEFAULT, args)
}

fun run(supplier: () -> Jooby, mode: ExecutionMode, args: Array<String>) {
  Jooby.run(supplier, mode, args)
}
