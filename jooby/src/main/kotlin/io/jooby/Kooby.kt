package io.jooby

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPEALIAS, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class RouterDsl

internal class JoobyCoroutineScope(coroutineContext: CoroutineContext) : CoroutineScope {
  override val coroutineContext = coroutineContext
}

open class CoroutineRouter (val router: Kooby) {
  @RouterDsl
  fun get(pattern: String = "/", coroutineScope: CoroutineScope? = null,
          coroutineStart: CoroutineStart = CoroutineStart.UNDISPATCHED,
          block: suspend Context.() -> Any): Route {
    return router.get(pattern) { ctx ->
      (coroutineScope ?: router.workerScope).launch(ContextCoroutineName, coroutineStart) {
        val result = ctx.block()
        ctx.render(result)
      }
    }.returnType(Job::class.java)
  }

  companion object {
    private val ContextCoroutineName = CoroutineName("ctx-handler")
  }
}

open class Kooby constructor() : Jooby() {

  val workerScope: CoroutineScope by lazy {
    val dispatcher: CoroutineDispatcher = worker().asCoroutineDispatcher()
    JoobyCoroutineScope(dispatcher)
  }

  constructor(init: Kooby.() -> Unit) : this() {
    this.init()
  }

  @RouterDsl
  fun coroutine(init: CoroutineRouter.() -> Unit): Kooby {
    CoroutineRouter(this).init()
    return this
  }

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

  companion object {
    private val ContextCoroutineName = CoroutineName("ctx-handler")
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
