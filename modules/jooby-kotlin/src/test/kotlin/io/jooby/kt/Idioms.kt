/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.kt

import io.jooby.Jooby
import io.jooby.RouterOption.IGNORE_CASE
import io.jooby.RouterOption.IGNORE_TRAILING_SLASH
import io.jooby.ServiceKey
import io.jooby.SslOptions
import java.nio.file.Paths
import java.time.Duration
import kotlinx.coroutines.delay

/**
 * Kotlin DLS in action, this class does nothing but we need it to make sure Kotlin version compiles
 * sucessfully.
 */
class Idioms :
  Kooby({
    services.put(Jooby::class, this)
    services.put(ServiceKey.key(Jooby::class.java, "name"), this)
    /** Services: */
    require<Jooby>()

    require<Jooby>("name")

    require(Jooby::class)

    require(Jooby::class, "name")

    services.get(Jooby::class)

    services.getOrNull(Jooby::class)

    /** Options: */
    serverOptions {
      bufferSize = 8194
      ioThreads = 8
      compressionLevel = 6
      defaultHeaders = false
      maxRequestSize = 8000
      port = 8080
      server = "server"
      workerThreads = 99
      securePort = 8443
      ssl = SslOptions().apply { type = "PKCS12" }
    }

    routerOptions(IGNORE_CASE, IGNORE_TRAILING_SLASH)

    setHiddenMethod { ctx -> ctx.header("").toOptional() }

    environmentOptions {
      this.activeNames = listOf("foo")
      this.basedir = Paths.get(".").toString()
      this.filename = "myfile"
    }

    cors {
      this.exposedHeaders = listOf("f")
      this.headers = listOf("d")
      this.maxAge = Duration.ZERO
      this.methods = listOf("GET")
      this.origin = listOf("*")
      this.useCredentials = true
    }

    /** Value DSL: */
    get("/") {
      val query = ctx.query()
      val name: String by ctx.query
      val n1 = query["name"].value()
      val n2 = query[0].value()
      val n3 = query.to<Int>()
      val n4 = ctx.query["name"] to Int::class
      val n5 = ctx.form("name") to String::class
      val n6 = query to Int::class

      println(n1 + n2 + n3 + n4 + n5 + name + n6)
      ctx
    }

    get("/attributes") { "some" }.attribute("k", "v")

    /** Router DSL: */
    before { ctx.path() }
    after { ctx.getRequestPath() }
    use { next.apply(ctx) }
    get("/") { ctx.path() }
    post("/") { ctx.path() }
    put("/") { ctx.path() }
    patch("/") { ctx.path() }
    delete("/") { ctx.path() }
    options("/") { ctx.path() }
    trace("/") { ctx.path() }
    /** Coroutine: */
    coroutine {
      get("/") { "Hi Kotlin!" }

      get("/suspend") {
        delay(100)
        "Hi Coroutine"
      }

      get("/ctx-access") { ctx.getRequestPath() }
    }

    install(::SubApp)

    install("/with-path", ::SubApp)

    /** WebSocket: */
    ws("/ws") {
      configurer.onConnect { ws -> ws.send("SS") }
      configurer.onMessage { ws, message ->
        val value = message.to<IdiomsPojo>()
        ws.render(value)
      }
    }
  })

class IdiomsController {}

class IdiomsPojo {}
