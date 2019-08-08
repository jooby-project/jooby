package io.jooby

import kotlinx.coroutines.delay
import java.nio.file.Paths
import java.time.Duration

/**
 * Kotlin DLS in action, this class does nothing but we need it to make sure Kotlin version
 * compiles sucessfully.
 */
class Idioms : Kooby({

  /** Services: */
  val s = require<Jooby>()
  println(s)

  val named = require<Jooby>("name")
  println(named)

  val sklass = require(Jooby::class)
  println(sklass)

  val sklassname = require(Jooby::class, "name")
  println(sklassname)

  val j1 = services.get(Jooby::class)
  println(j1)

  val j2 = services.getOrNull(Jooby::class)
  println(j2)

  /** Options: */
  serverOptions {
    bufferSize = 8194
    directBuffers = true
    ioThreads = 8
    gzip = true
    defaultHeaders = false
    maxRequestSize = 8000
    port = 8080
    server = "server"
    singleLoop = true
    workerThreads = 99
  }

  routerOptions {
    this.ignoreCase = true
    this.ignoreTrailingSlash = true
  }

  environmentOptions {
    this.activeNames = listOf("foo")
    this.basedir = Paths.get(".").toString()
    this.filename = "myfile"
  }

  val cors = cors {
    this.exposedHeaders = listOf("f")
    this.headers = listOf("d")
    this.maxAge = Duration.ZERO
    this.methods = listOf("GET")
    this.origin = listOf("*")
    this.useCredentials = true
  }
  println(cors)

  /** Value DSL: */
  get("/") {
    val query = ctx.query()
    val name: String by ctx.query
    val n1 = query["name"].value()
    val n2 = query[0].value()
    val n3 = query.to<Int>()
    val n4 = ctx.query["name"] to Int::class
    val n5 = ctx.form("name") to String::class
    val n6 = query.to(Int::class)

    println(n1 + n2 + n3 + n4 + n5 + name + n6)
    ctx
  }

  /** Router DSL: */
  before {
    ctx.path()
  }
  after {
    ctx.pathString()
  }
  decorator {
    next.apply(ctx)
  }
  get("/") {
    ctx.path()
  }
  post("/") {
    ctx.path()
  }
  put("/") {
    ctx.path()
  }
  patch("/") {
    ctx.path()
  }
  delete("/") {
    ctx.path()
  }
  options("/") {
    ctx.path()
  }
  trace("/") {
    ctx.path()
  }
  // mvc
  mvc(IdiomsController::class)

  mvc(IdiomsController::class, ::IdiomsController)

  /** Coroutine: */
  coroutine {
    get { "Hi Kotlin!" }

    get("/suspend") {
      delay(100)
      "Hi Coroutine"
    }

    get("/ctx-access") {
      ctx.pathString()
    }
  }
})

class IdiomsController {}
