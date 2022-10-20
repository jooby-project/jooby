/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package apps

import io.jooby.ExecutionMode
import io.jooby.Kooby
import io.jooby.runApp
import kotlinx.coroutines.delay

/** Class version: */
class App :
  Kooby({
    coroutine {
      get("/") { "Hi Kotlin!" }

      get("/suspend") {
        delay(100)
        "Hi Coroutine"
      }

      get("/ctx-access") { ctx.getRequestPath() }
    }

    get("/ctx-arg") { ctx -> ctx.getRequestPath() }
  })

/** run class: */
fun runClass(args: Array<String>) {
  runApp(args, App::class)
}

/** run class with mode: */
fun runWithMode(args: Array<String>) {
  runApp(args, ExecutionMode.DEFAULT, App::class)
}

/** run inline: */
fun runInline(args: Array<String>) {
  runApp(args) {
    coroutine {
      get("/") { "Hi Kotlin!" }

      get("/suspend") {
        delay(100)
        "Hi Coroutine"
      }

      get("/ctx-access") { ctx.getRequestPath() }

      get("/ctx-arg") { ctx -> ctx.getRequestPath() }
    }
  }
}
